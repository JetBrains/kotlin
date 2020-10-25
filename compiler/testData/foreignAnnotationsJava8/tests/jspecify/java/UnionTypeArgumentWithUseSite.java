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
class UnionTypeArgumentWithUseSite {
  interface Super<T extends @Nullable Object> {
    void t(T t);

    void tUnspec(@NullnessUnspecified T t);

    void tUnionNull(@Nullable T t);
  }

  interface Sub extends Super<Object> {
    @Override
    void t(Object t);

    @Override
    // jspecify_nullness_not_enough_information
    void tUnspec(@NullnessUnspecified Object t);

    @Override
    void tUnionNull(@Nullable Object t);
  }

  interface SubUnspec extends Super<@NullnessUnspecified Object> {
    @Override
    // jspecify_nullness_not_enough_information
    void t(@NullnessUnspecified Object t);

    @Override
    // jspecify_nullness_not_enough_information
    void tUnspec(@NullnessUnspecified Object t);

    @Override
    void tUnionNull(@Nullable Object t);
  }

  interface SubUnionNull extends Super<@Nullable Object> {
    @Override
    void t(@Nullable Object t);

    @Override
    void tUnspec(@Nullable Object t);

    @Override
    void tUnionNull(@Nullable Object t);
  }

  interface SubWeaker extends Super<Object> {
    @Override
    // jspecify_nullness_not_enough_information
    void tUnspec(Object t);

    @Override
    // jspecify_nullness_mismatch
    void tUnionNull(Object t);
  }

  interface SubWeakerUnspec extends Super<@NullnessUnspecified Object> {
    @Override
    // jspecify_nullness_not_enough_information
    void t(Object t);

    @Override
    // jspecify_nullness_not_enough_information
    void tUnspec(Object t);

    @Override
    // jspecify_nullness_mismatch
    void tUnionNull(Object t);
  }

  interface SubWeakerUnionNull extends Super<@Nullable Object> {
    @Override
    // jspecify_nullness_mismatch
    void t(Object t);

    @Override
    // jspecify_nullness_mismatch
    void tUnspec(Object t);

    @Override
    // jspecify_nullness_mismatch
    void tUnionNull(Object t);
  }
}
