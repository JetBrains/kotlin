/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.reflect.jvm.internal;

import kotlin.jvm.internal.*;
import kotlin.reflect.*;
import kotlin.reflect.full.KClassifiers;
import kotlin.reflect.jvm.ReflectLambdaKt;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * @suppress
 */
@SuppressWarnings({"UnusedDeclaration", "unchecked", "rawtypes"})
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
        // moduleName is unused deliberately and only left in public ABI
        return ClassCachesKt.getOrCreateKotlinPackage(javaClass);
    }

    @Override
    public KClass getOrCreateKotlinClass(Class javaClass) {
        return ClassCachesKt.getOrCreateKotlinClass(javaClass);
    }

    @Override
    public KClass getOrCreateKotlinClass(Class javaClass, String internalName) {
        return ClassCachesKt.getOrCreateKotlinClass(javaClass);
    }

    @Override
    public String renderLambdaToString(Lambda lambda) {
        return renderLambdaToString((FunctionBase) lambda);
    }

    @Override
    public String renderLambdaToString(FunctionBase lambda) {
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
        return new KFunctionImpl(getOwner(f), f.getName(), f.getSignature(), f.getBoundReceiver());
    }

    // Properties

    @Override
    public KProperty0 property0(PropertyReference0 p) {
        return new KProperty0Impl(getOwner(p), p.getName(), p.getSignature(), p.getBoundReceiver());
    }

    @Override
    public KMutableProperty0 mutableProperty0(MutablePropertyReference0 p) {
        return new KMutableProperty0Impl(getOwner(p), p.getName(), p.getSignature(), p.getBoundReceiver());
    }

    @Override
    public KProperty1 property1(PropertyReference1 p) {
        return new KProperty1Impl(getOwner(p), p.getName(), p.getSignature(), p.getBoundReceiver());
    }

    @Override
    public KMutableProperty1 mutableProperty1(MutablePropertyReference1 p) {
        return new KMutableProperty1Impl(getOwner(p), p.getName(), p.getSignature(), p.getBoundReceiver());
    }

    @Override
    public KProperty2 property2(PropertyReference2 p) {
        return new KProperty2Impl(getOwner(p), p.getName(), p.getSignature());
    }

    @Override
    public KMutableProperty2 mutableProperty2(MutablePropertyReference2 p) {
        return new KMutableProperty2Impl(getOwner(p), p.getName(), p.getSignature());
    }

    private static KDeclarationContainerImpl getOwner(CallableReference reference) {
        KDeclarationContainer owner = reference.getOwner();
        return owner instanceof KDeclarationContainerImpl ? ((KDeclarationContainerImpl) owner) : EmptyContainerForLocal.INSTANCE;
    }

    // typeOf

    @Override
    public KType typeOf(KClassifier klass, List<KTypeProjection> arguments, boolean isMarkedNullable) {
        return KClassifiers.createType(klass, arguments, isMarkedNullable, Collections.<Annotation>emptyList());
    }

    @Override
    public KTypeParameter typeParameter(Object container, String name, KVariance variance, boolean isReified) {
        List<KTypeParameter> typeParameters;
        if (container instanceof KClass) {
            typeParameters = ((KClass<?>) container).getTypeParameters();
        }
        else if (container instanceof KCallable) {
            typeParameters = ((KCallable<?>) container).getTypeParameters();
        }
        else {
            throw new IllegalArgumentException("Type parameter container must be a class or a callable: " + container);
        }
        for (KTypeParameter typeParameter : typeParameters) {
            if (typeParameter.getName().equals(name)) return typeParameter;
        }
        throw new IllegalArgumentException("Type parameter " + name + " is not found in container: " + container);
    }

    @Override
    public void setUpperBounds(KTypeParameter typeParameter, List<KType> bounds) {
        // Do nothing. KTypeParameterImpl implementation will load upper bounds from the metadata.
    }

    // @Override // JPS
    public KType platformType(KType lowerBound, KType upperBound) {
        return TypeOfImplKt.createPlatformKType(lowerBound, upperBound);
    }

    // @Override // JPS
    public KType mutableCollectionType(KType type) {
        return TypeOfImplKt.createMutableCollectionKType(type);
    }

    // @Override // JPS
    public KType nothingType(KType type) {
        return TypeOfImplKt.createNothingType(type);
    }

    // Misc

    public static void clearCaches() {
        ClassCachesKt.clearCaches();
        ModuleByClassLoaderKt.clearModuleByClassLoaderCache();
    }
}
