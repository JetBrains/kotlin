/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:Suppress("unused")

package org.jetbrains.kotlin.scripts

open class TestDSLClass

fun TestDSLClass.fibCombine(f: (Int) -> Int, n: Int) = if (n < 2) 1 else f(n - 1) + f(n - 2)

interface TestDSLInterface

fun TestDSLInterface.fibCombine(f: (Int) -> Int, n: Int) = if (n < 2) 1 else f(n - 1) + f(n - 2)

open class TestDSLClassWithParam(val offset: Int)

fun TestDSLClassWithParam.fibCombine(f: (Int) -> Int, n: Int) = if (n < 2) offset else f(n - 1) + f(n - 2)

open class TestClassWithOverridableProperty(open val num: Int)