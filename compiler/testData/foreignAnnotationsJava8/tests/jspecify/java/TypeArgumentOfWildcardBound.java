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
class TypeArgumentOfWildcardBound {
  interface Supplier<T extends @Nullable Object> {
    T get();
  }

  Supplier<Object> x1(Supplier<? extends Supplier<Object>> s) {
    return s.get();
  }

  Supplier<@NullnessUnspecified Object> x2(Supplier<? extends Supplier<Object>> s) {
    // jspecify_nullness_not_enough_information
    return s.get();
  }

  Supplier<@Nullable Object> x3(Supplier<? extends Supplier<Object>> s) {
    // jspecify_nullness_mismatch
    return s.get();
  }

  Supplier<Object> x4(Supplier<? extends Supplier<@NullnessUnspecified Object>> s) {
    // jspecify_nullness_not_enough_information
    return s.get();
  }

  Supplier<@NullnessUnspecified Object> x5(
      Supplier<? extends Supplier<@NullnessUnspecified Object>> s) {
    // jspecify_nullness_not_enough_information
    return s.get();
  }

  Supplier<@Nullable Object> x6(Supplier<? extends Supplier<@NullnessUnspecified Object>> s) {
    // jspecify_nullness_not_enough_information
    return s.get();
  }

  Supplier<Object> x7(Supplier<? extends Supplier<@Nullable Object>> s) {
    // jspecify_nullness_mismatch
    return s.get();
  }

  Supplier<@NullnessUnspecified Object> x8(Supplier<? extends Supplier<@Nullable Object>> s) {
    // jspecify_nullness_not_enough_information
    return s.get();
  }

  Supplier<@Nullable Object> x9(Supplier<? extends Supplier<@Nullable Object>> s) {
    return s.get();
  }
}
