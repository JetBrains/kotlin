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
class TypeArgumentOfTypeVariableBound {
  interface Supplier<T extends @Nullable Object> {
    T get();
  }

  <S extends Supplier<Object>> Supplier<Object> x1(S s) {
    return s;
  }

  <S extends Supplier<Object>> Supplier<@NullnessUnspecified Object> x2(S s) {
    // jspecify_nullness_not_enough_information
    return s;
  }

  <S extends Supplier<Object>> Supplier<@Nullable Object> x3(S s) {
    // jspecify_nullness_mismatch
    return s;
  }

  <S extends Supplier<@NullnessUnspecified Object>> Supplier<Object> x4(S s) {
    // jspecify_nullness_not_enough_information
    return s;
  }

  <S extends Supplier<@NullnessUnspecified Object>> Supplier<@NullnessUnspecified Object> x5(S s) {
    // jspecify_nullness_not_enough_information
    return s;
  }

  <S extends Supplier<@NullnessUnspecified Object>> Supplier<@Nullable Object> x6(S s) {
    // jspecify_nullness_not_enough_information
    return s;
  }

  <S extends Supplier<@Nullable Object>> Supplier<Object> x7(S s) {
    // jspecify_nullness_mismatch
    return s;
  }

  <S extends Supplier<@Nullable Object>> Supplier<@NullnessUnspecified Object> x8(S s) {
    // jspecify_nullness_not_enough_information
    return s;
  }

  <S extends Supplier<@Nullable Object>> Supplier<@Nullable Object> x9(S s) {
    return s;
  }
}
