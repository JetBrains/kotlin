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

package test;

import java.lang.String;
import java.util.ArrayList;

import jet.runtime.typeinfo.KotlinSignature;

public class PropertyArrayTypes<T> {
    @KotlinSignature("fun PropertyArrayTypes(genericTypeParam : T)")
    public PropertyArrayTypes(T genericTypeParam) {
        // For initializing genericType field in kotlin
    }

    @KotlinSignature("var arrayOfArrays : Array<Array<String>>")
    public String[][] arrayOfArrays;

    @KotlinSignature("var array : Array<String>")
    public String[] array;

    @KotlinSignature("var genericArray : Array<T>")
    public T[] genericArray;
}
