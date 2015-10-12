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
    public KDeclarationContainer getOrCreateKotlinPackage(Class javaClass, String moduleName) {
        return new KPackageImpl(javaClass, moduleName);
    }

    @Override
    public KClass getOrCreateKotlinClass(Class javaClass) {
        return InternalPackage.getOrCreateKotlinClass(javaClass);
    }

    // Functions

    @Override
    public KFunction function(FunctionReference f) {
        return new KFunctionFromReferenceImpl(f);
    }

    // Properties

    @Override
    public KProperty0 property0(PropertyReference0 p) {
        return new KProperty0FromReferenceImpl(p);
    }

    @Override
    public KMutableProperty0 mutableProperty0(MutablePropertyReference0 p) {
        return new KMutableProperty0FromReferenceImpl(p);
    }

    @Override
    public KProperty1 property1(PropertyReference1 p) {
        return new KProperty1FromReferenceImpl(p);
    }

    @Override
    public KMutableProperty1 mutableProperty1(MutablePropertyReference1 p) {
        return new KMutableProperty1FromReferenceImpl(p);
    }

    @Override
    public KProperty2 property2(PropertyReference2 p) {
        // TODO: support member extension property references
        return p;
    }

    @Override
    public KMutableProperty2 mutableProperty2(MutablePropertyReference2 p) {
        // TODO: support member extension property references
        return p;
    }

    // Deprecated

    @Override
    public KClass foreignKotlinClass(Class javaClass) {
        return getOrCreateKotlinClass(javaClass);
    }

    @Override
    public KPackage createKotlinPackage(Class javaClass, String moduleName) {
        return new KPackageImpl(javaClass, moduleName);
    }
}
