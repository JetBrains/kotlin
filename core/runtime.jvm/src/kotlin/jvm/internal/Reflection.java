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

package kotlin.jvm.internal;

import kotlin.reflect.*;

/**
 * This class serves as a facade to the actual reflection implementation. JVM back-end generates calls to static methods of this class
 * on any reflection-using construct.
 */
public class Reflection {
    private static final ReflectionFactory factory;

    static {
        ReflectionFactory impl;
        try {
            Class<?> implClass = Class.forName("kotlin.reflect.jvm.internal.ReflectionFactoryImpl");
            impl = (ReflectionFactory) implClass.newInstance();
        }
        catch (ClassCastException e) { impl = null; }
        catch (ClassNotFoundException e) { impl = null; }
        catch (InstantiationException e) { impl = null; }
        catch (IllegalAccessException e) { impl = null; }

        factory = impl != null ? impl : new ReflectionFactory();
    }

    public static KClass createKotlinClass(Class javaClass) {
        return factory.createKotlinClass(javaClass);
    }

    public static KPackage createKotlinPackage(Class javaClass) {
        return factory.createKotlinPackage(javaClass);
    }

    public static KClass foreignKotlinClass(Class javaClass) {
        return factory.foreignKotlinClass(javaClass);
    }

    public static KMemberProperty memberProperty(String name, KClass owner) {
        return factory.memberProperty(name, owner);
    }

    public static KMutableMemberProperty mutableMemberProperty(String name, KClass owner) {
        return factory.mutableMemberProperty(name, owner);
    }

    public static KTopLevelVariable topLevelVariable(String name, KPackage owner) {
        return factory.topLevelVariable(name, owner);
    }

    public static KMutableTopLevelVariable mutableTopLevelVariable(String name, KPackage owner) {
        return factory.mutableTopLevelVariable(name, owner);
    }

    public static KTopLevelExtensionProperty topLevelExtensionProperty(String name, KPackage owner, Class receiver) {
        return factory.topLevelExtensionProperty(name, owner, receiver);
    }

    public static KMutableTopLevelExtensionProperty mutableTopLevelExtensionProperty(String name, KPackage owner, Class receiver) {
        return factory.mutableTopLevelExtensionProperty(name, owner, receiver);
    }
}
