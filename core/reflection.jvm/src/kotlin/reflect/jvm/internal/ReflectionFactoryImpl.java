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

import kotlin.jvm.internal.ReflectionFactory;
import kotlin.reflect.*;

@SuppressWarnings({"UnusedDeclaration", "unchecked"})
public class ReflectionFactoryImpl extends ReflectionFactory {
    @Override
    public KClass createKotlinClass(Class javaClass) {
        return new KClassImpl(javaClass);
    }

    @Override
    public KPackage createKotlinPackage(Class javaClass) {
        return new KPackageImpl(javaClass);
    }

    @Override
    public KClass foreignKotlinClass(Class javaClass) {
        return InternalPackage.foreignKotlinClass(javaClass);
    }

    @Override
    public KMemberProperty memberProperty(String name, KClass owner) {
        return ((KClassImpl) owner).memberProperty(name);
    }

    @Override
    public KMutableMemberProperty mutableMemberProperty(String name, KClass owner) {
        return ((KClassImpl) owner).mutableMemberProperty(name);
    }

    @Override
    public KTopLevelVariable topLevelVariable(String name, KPackage owner) {
        return ((KPackageImpl) owner).topLevelVariable(name);
    }

    @Override
    public KMutableTopLevelVariable mutableTopLevelVariable(String name, KPackage owner) {
        return ((KPackageImpl) owner).mutableTopLevelVariable(name);
    }

    @Override
    public KTopLevelExtensionProperty topLevelExtensionProperty(String name, KPackage owner, Class receiver) {
        return ((KPackageImpl) owner).topLevelExtensionProperty(name, receiver);
    }

    @Override
    public KMutableTopLevelExtensionProperty mutableTopLevelExtensionProperty(String name, KPackage owner, Class receiver) {
        return ((KPackageImpl) owner).mutableTopLevelExtensionProperty(name, receiver);
    }
}
