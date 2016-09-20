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

package kotlin.jvm

/**
 * Instructs the Kotlin compiler to treat annotated Java class as pure implementation of given Kotlin interface.
 * "Pure" means here that each type parameter of class becomes non-platform type argument of that interface.
 *
 * Example:
 *
 * class MyList<T> extends AbstractList<T> { ... }
 *
 * Methods defined in MyList<T> use T as platform, i.e. it's possible to perform unsafe operation in Kotlin:
 *  MyList<Int>().add(null) // compiles
 *
 * @PurelyImplements("kotlin.MutableList")
 * class MyPureList<T> extends AbstractList<T> { ... }
 *
 * Methods defined in MyPureList<T> overriding methods in MutableList use T as non-platform types:
 *  MyList<Int>().add(null) // Error
 *  MyList<Int?>().add(null) // Ok
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class PurelyImplements(val value: String)
