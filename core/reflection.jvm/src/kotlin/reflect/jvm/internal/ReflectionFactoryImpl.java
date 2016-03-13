/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal;

import kotlin.jvm.internal.*;
import kotlin.reflect.*;
import kotlin.reflect.jvm.ReflectLambdaKt;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;

/**
 * @suppress
 */
@SuppressWarnings({"UnusedDeclaration", "unchecked"})
public class ReflectionFactoryImpl extends ReflectionFactory {
    @Override
    public KClass createKotlinClass(Class javaClass) {
        return new KClassImpl(javaClass);
    }

    @Override
    public KClass createKotlinClass(Class javaClass, String internalName) {
        return new KClassImpl(javaClass);
    }

    @Override
    public KDeclarationContainer getOrCreateKotlinPackage(Class javaClass, String moduleName) {
        return new KPackageImpl(javaClass, moduleName);
    }

    @Override
    public KClass getOrCreateKotlinClass(Class javaClass) {
        return KClassCacheKt.getOrCreateKotlinClass(javaClass);
    }

    @Override
    public KClass getOrCreateKotlinClass(Class javaClass, String internalName) {
        return KClassCacheKt.getOrCreateKotlinClass(javaClass);
    }

    @Override
    public String renderLambdaToString(Lambda lambda) {
        KFunction kFunction = ReflectLambdaKt.reflect(lambda);
        if (kFunction != null) {
            KFunctionImpl impl = UtilKt.asKFunctionImpl(kFunction);
            if (impl != null) {
                return ReflectionObjectRenderer.INSTANCE.renderLambda(impl.getDescriptor());
            }
        }
        return super.renderLambdaToString(lambda);
    }

    // Functions

    @Override
    public KFunction function(FunctionReference f) {
        return new KFunctionFromReferenceImpl(f);
    }

    // Properties

    @Override
    public KProperty0 property0(PropertyReference0 p) {
        return p instanceof PropertyReference0Impl ? new KProperty0Augmented(p) : new KProperty0FromReferenceImpl(p);
    }

    @Override
    public KMutableProperty0 mutableProperty0(MutablePropertyReference0 p) {
        return p instanceof MutablePropertyReference0Impl ? new KMutableProperty0Augmented(p) : new KMutableProperty0FromReferenceImpl(p);
    }

    @Override
    public KProperty1 property1(PropertyReference1 p) {
        return p instanceof PropertyReference1Impl ? new KProperty1Augmented(p) : new KProperty1FromReferenceImpl(p);
    }

    @Override
    public KMutableProperty1 mutableProperty1(MutablePropertyReference1 p) {
        return p instanceof MutablePropertyReference1Impl ? new KMutableProperty1Augmented(p) : new KMutableProperty1FromReferenceImpl(p);
    }

    @Override
    public KProperty2 property2(PropertyReference2 p) {
        return p instanceof PropertyReference2Impl ? new KProperty2Augmented(p) : new KProperty2FromReferenceImpl(p);
    }

    @Override
    public KMutableProperty2 mutableProperty2(MutablePropertyReference2 p) {
        return p instanceof MutablePropertyReference2Impl ? new KMutableProperty2Augmented(p) : new KMutableProperty2FromReferenceImpl(p);
    }
}
