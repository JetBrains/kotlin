/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

fun f(x: Int, fooBar1: String, fooBar2: String){}

fun g(someBar0: String, someBar1: String, someBar2: String, fooBar: String, fooBar0: String, fooBar1: String, fooBar2: String) {
    f(1, <caret>)
}

// ORDER: fooBar1, fooBar, fooBar0, fooBar2, someBar1, someBar0, someBar2
