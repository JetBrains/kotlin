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
class SuperTypeVariableUnionNull {
  <T> void implicitlyObjectBounded(
      Lib<? super @Nullable T> lib, T t, @NullnessUnspecified T tUnspec, @Nullable T tUnionNull) {
    lib.useT(t);

    lib.useT(tUnspec);

    lib.useT(tUnionNull);

    //

    lib.useTUnspec(t);

    lib.useTUnspec(tUnspec);

    lib.useTUnspec(tUnionNull);

    //

    lib.useTUnionNull(t);

    lib.useTUnionNull(tUnspec);

    lib.useTUnionNull(tUnionNull);
  }

  <T extends Object> void explicitlyObjectBounded(
      Lib<? super @Nullable T> lib, T t, @NullnessUnspecified T tUnspec, @Nullable T tUnionNull) {
    lib.useT(t);

    lib.useT(tUnspec);

    lib.useT(tUnionNull);

    //

    lib.useTUnspec(t);

    lib.useTUnspec(tUnspec);

    lib.useTUnspec(tUnionNull);

    //

    lib.useTUnionNull(t);

    lib.useTUnionNull(tUnspec);

    lib.useTUnionNull(tUnionNull);
  }

  <T extends @NullnessUnspecified Object> void unspecBounded(
      Lib<? super @Nullable T> lib, T t, @NullnessUnspecified T tUnspec, @Nullable T tUnionNull) {
    lib.useT(t);

    lib.useT(tUnspec);

    lib.useT(tUnionNull);

    //

    lib.useTUnspec(t);

    lib.useTUnspec(tUnspec);

    lib.useTUnspec(tUnionNull);

    //

    lib.useTUnionNull(t);

    lib.useTUnionNull(tUnspec);

    lib.useTUnionNull(tUnionNull);
  }

  <T extends @Nullable Object> void nullableBounded(
      Lib<? super @Nullable T> lib, T t, @NullnessUnspecified T tUnspec, @Nullable T tUnionNull) {
    lib.useT(t);

    lib.useT(tUnspec);

    lib.useT(tUnionNull);

    //

    lib.useTUnspec(t);

    lib.useTUnspec(tUnspec);

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
