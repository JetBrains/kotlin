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
class SuperObjectUnspec {
  void foo(
      Lib<? super @NullnessUnspecified Object> lib,
      Object t,
      @NullnessUnspecified Object tUnspec,
      @Nullable Object tUnionNull) {
    lib.useT(t);

    // jspecify_nullness_not_enough_information
    lib.useT(tUnspec);

    // jspecify_nullness_not_enough_information
    lib.useT(tUnionNull);

    //

    lib.useTUnspec(t);

    // jspecify_nullness_not_enough_information
    lib.useTUnspec(tUnspec);

    // jspecify_nullness_not_enough_information
    lib.useTUnspec(tUnionNull);

    //

    lib.useTUnionNull(t);

    lib.useTUnionNull(tUnspec);

    lib.useTUnionNull(tUnionNull);
  }

  interface Lib<T extends @Nullable Object> {
    void useT(T t);

    void useTUnspec(@NullnessUnspecified T t);

    void useTUnionNull(@Nullable T t);
  }
}
