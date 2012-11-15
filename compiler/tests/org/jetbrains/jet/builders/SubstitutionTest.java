


/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.builders;

import beans.JetTypeBean;
import beans.TypeConstructorBean;
import beans.TypeParameterDescriptorBean;
import beans.TypeProjectionBean;
import beans.impl.JetTypeBeanImpl;
import beans.impl.TypeProjectionBeanImpl;
import beans.references.ClassifierDescriptorBeanReference;
import beans.references.TypeConstructorBeanReference;
import beans.references.TypeParameterDescriptorBeanReference;
import beans.references.impl.LiteralTypeConstructorBeanReference;
import beans.util.CopyProcessor;
import beans.util.DataToBeanProcessor;
import beans.util.ToString;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForTests;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeSubstitutor.*;

public class SubstitutionTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    public static class Substitutor extends CopyProcessor {
        private final Map<Object, TypeProjectionBean> constructorToSubstitution;

        public Substitutor(Map<Object, TypeProjectionBean> substitution) {
            constructorToSubstitution = substitution;
        }

        @Override
        public TypeProjectionBean processTypeProjection(TypeProjectionBean originalProjection) {
            TypeConstructorBeanReference typeConstructorRef = originalProjection.getType().getConstructor();
            Object constructor = typeConstructorRef.resolveTo(Object.class);
            TypeProjectionBean replacement = constructorToSubstitution.get(constructor);
            if (replacement != null) {
                switch (conflictType(originalProjection.getProjectionKind(), replacement.getProjectionKind())) {
                    case OUT_IN_IN_POSITION:
                        throw new SubstitutionException("Out-projection in in-position");
                    case IN_IN_OUT_POSITION:
                        replacement = makeStarProjectionBean(typeConstructorRef);
                        break;
                }
                // We do not substitute in the result, to do so:
                // replacement = processTypeProjection(replacement);
                replacement.getType().setNullable(replacement.getType().isNullable() || originalProjection.getType().isNullable());
                return new TypeProjectionBeanImpl()
                        .setProjectionKind(combine(originalProjection.getProjectionKind(), replacement.getProjectionKind()))
                        .setType(replacement.getType());
            }
            return super.processTypeProjection(originalProjection);
        }

        @Override
        protected void processJetType_Arguments(@NotNull JetTypeBean in, JetTypeBean out) {
            List<? extends TypeProjectionBean> arguments = in.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                TypeParameterDescriptorBeanReference typeParameterRef = getTypeParameterByIndex(in.getConstructor());
                TypeProjectionBean typeArgument = arguments.get(i);

                TypeProjectionBean substitutedTypeArgument = convertJetType_Arguments(typeArgument);

                switch (conflictType(getVariance(typeParameterRef), substitutedTypeArgument.getProjectionKind())) {
                    case OUT_IN_IN_POSITION:
                        substitutedTypeArgument = inProjectionBean(getLowerBoundsAsType(typeParameterRef));
                        break;
                    case IN_IN_OUT_POSITION:
                        substitutedTypeArgument = makeStarProjectionBean(getTypeConstructor(typeParameterRef));
                        break;
                }

                out.addToArguments(substitutedTypeArgument);
            }
        }

        private static TypeConstructorBeanReference getTypeConstructor(TypeParameterDescriptorBeanReference ref) {
            TypeParameterDescriptorBean bean = ref.resolveTo(TypeParameterDescriptorBean.class);
            if (bean != null) {
                return new LiteralTypeConstructorBeanReference(bean.getTypeConstructor());
            }

            TypeParameterDescriptor trueObject = ref.resolveTo(TypeParameterDescriptor.class);
            if (trueObject != null) {
                return new LiteralTypeConstructorBeanReference(trueObject.getTypeConstructor());
            }

            throw new IllegalStateException("Cannot resolve reference to descriptor od bean: " + ref.resolveTo(Object.class));
        }

        private static JetTypeBean getLowerBoundsAsType(TypeParameterDescriptorBeanReference ref) {
            // TODO: actual lower bounds
            return new DataToBeanProcessor().processJetType(
                    KotlinBuiltIns.getInstance().getNothingType()
            );
        }

        private static Variance getVariance(TypeParameterDescriptorBeanReference ref) {
            TypeParameterDescriptorBean bean = ref.resolveTo(TypeParameterDescriptorBean.class);
            if (bean != null) {
                return bean.getVariance();
            }

            TypeParameterDescriptor trueObject = ref.resolveTo(TypeParameterDescriptor.class);
            if (trueObject != null) {
                return trueObject.getVariance();
            }

            throw new IllegalStateException("Cannot resolve reference to descriptor od bean: " + ref.resolveTo(Object.class));
        }

        private static TypeProjectionBean makeStarProjectionBean(TypeConstructorBeanReference ref) {
            TypeConstructorBean bean = ref.resolveTo(TypeConstructorBean.class);
            if (bean != null) {
                return makeStarProjectionBean(bean.getDeclarationDescriptor());
            }

            TypeConstructor trueObject = ref.resolveTo(TypeConstructor.class);
            if (trueObject != null) {
                return trueObject.getVariance();
            }

            throw new IllegalStateException("Cannot resolve reference to descriptor od bean: " + ref.resolveTo(Object.class));
        }

        private static TypeProjectionBean makeStarProjectionBean(ClassifierDescriptorBeanReference ref) {

        }

        private static TypeParameterDescriptorBeanReference getTypeParameterByIndex(TypeConstructorBeanReference constructor) {
            throw new UnsupportedOperationException(); // TODO
        }
    }

    public void testOneType() throws Exception {
        ClassDescriptor arbitraryClass = KotlinBuiltIns.getInstance().getArray();

        InjectorForTests injectorForTests = new InjectorForTests(getProject());

        JetNamedFunction functionF = JetPsiFactory.createFunction(getProject(), "fun <T, R> f()");
        BindingTrace trace = new BindingTraceContext();
        SimpleFunctionDescriptor functionDescriptor = injectorForTests.getDescriptorResolver().resolveFunctionDescriptor(
                arbitraryClass,
                KotlinBuiltIns.getInstance().getBuiltInsScope(),
                functionF,
                trace
        );

        WritableScope scope = new WritableScopeImpl(
                KotlinBuiltIns.getInstance().getBuiltInsScope(),
                arbitraryClass,
                RedeclarationHandler.THROW_EXCEPTION,
                "test scope"
        );
        TypeParameterDescriptor typeParameterT = functionDescriptor.getTypeParameters().get(0);
        TypeParameterDescriptor typeParameterR = functionDescriptor.getTypeParameters().get(1);
        scope.addClassifierDescriptor(typeParameterT);
        scope.addClassifierDescriptor(typeParameterR);
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        JetType subjectType = createType("Map<List<T>, Map<out R, String>>", injectorForTests, trace, scope);
        JetType typeForR = createType("List<T>", injectorForTests, trace, scope);

        assertTrue("Errors:" + trace.getBindingContext().getDiagnostics(),
                   trace.getBindingContext().getDiagnostics().isEmpty());

        JetTypeBean typeBean = new DataToBeanProcessor().processJetType(subjectType);
        JetTypeBean typeBeanForR = new DataToBeanProcessor().processJetType(typeForR);

        JetTypeBeanImpl intTypeBean =
                new JetTypeBeanImpl().setConstructor(new LiteralTypeConstructorBeanReference(KotlinBuiltIns.getInstance().getInt().getTypeConstructor()));



        Substitutor substitutor = new Substitutor(ImmutableMap.<Object, TypeProjectionBean>builder()
                                                          .put(typeParameterT.getTypeConstructor(), invariantProjectionBean(intTypeBean))
                                                          .put(typeParameterR.getTypeConstructor(), invariantProjectionBean(typeBeanForR))
                                                          .build());
        JetTypeBean result = substitutor.processJetType(typeBean);
        assertEquals(
                new ToString().processJetType(typeBean),
                new ToString().processJetType(result));
    }

    private TypeProjectionBean invariantProjectionBean(JetTypeBean intTypeBean) {
        return new TypeProjectionBeanImpl().setProjectionKind(Variance.INVARIANT).setType(intTypeBean);
    }

    private static TypeProjectionBean inProjectionBean(JetTypeBean intTypeBean) {
        return new TypeProjectionBeanImpl().setProjectionKind(Variance.IN_VARIANCE).setType(intTypeBean);
    }

    private JetType createType(String typeStr, InjectorForTests injectorForTests, BindingTrace trace, WritableScope scope) {
        JetTypeReference typeReference = JetPsiFactory.createType(getProject(), typeStr);
        return injectorForTests.getTypeResolver().resolveType(scope, typeReference, trace, false);
    }
}
