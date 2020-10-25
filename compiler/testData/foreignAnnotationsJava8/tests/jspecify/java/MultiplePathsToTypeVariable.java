/*
 * Copyright 2020 The JSpecify Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jspecify.annotations.DefaultNonNull;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullnessUnspecified;

@DefaultNonNull
class MultiplePathsToTypeVariable {
  interface TBounded<T, U extends T> {
    U get();
  }

  interface TUnspecBounded<T, U extends @NullnessUnspecified T> {
    U get();
  }

  interface TUnionNullBounded<T, U extends @Nullable T> {
    U get();
  }

  <T> Object x0(TBounded<T, ? extends T> t) {
    return t.get();
  }

  <T> Object x1(TBounded<T, ? extends @NullnessUnspecified T> t) {
    return t.get();
  }

  <T> Object x2(TBounded<T, ? extends @Nullable T> t) {
    return t.get();
  }

  <T> Object x3(TUnspecBounded<T, ? extends T> t) {
    return t.get();
  }

  <T> Object x4(TUnspecBounded<T, ? extends @NullnessUnspecified T> t) {
    // jspecify_nullness_not_enough_information
    return t.get();
  }

  <T> Object x5(TUnspecBounded<T, ? extends @Nullable T> t) {
    // jspecify_nullness_not_enough_information
    return t.get();
  }

  <T> Object x6(TUnionNullBounded<T, ? extends T> t) {
    return t.get();
  }

  <T> Object x7(TUnionNullBounded<T, ? extends @NullnessUnspecified T> t) {
    // jspecify_nullness_not_enough_information
    return t.get();
  }

  <T> Object x8(TUnionNullBounded<T, ? extends @Nullable T> t) {
    // jspecify_nullness_mismatch
    return t.get();
  }
}
