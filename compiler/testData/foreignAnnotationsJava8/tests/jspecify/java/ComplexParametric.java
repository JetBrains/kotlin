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
class ComplexParametric {
  interface SuperSuper<T extends @Nullable Object> {
    Lib<T> t();

    Lib<@NullnessUnspecified T> tUnspec();

    Lib<@Nullable T> tUnionNull();

    void checkT(Lib<T> lib);

    void checkTUnspec(Lib<@NullnessUnspecified T> lib);

    void checkTUnionNull(Lib<@Nullable T> lib);

    // And some method that do not use T:

    void checkNeverNull(Lib<? extends Object> lib);

    <U> void checkUnspecNull(Lib<@NullnessUnspecified U> lib);
  }

  interface SuperNeverNever<T extends Object & Foo> extends SuperSuper<T> {
    default void x() {
      checkNeverNull(t());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(t());
      checkT(t());
      // jspecify_nullness_not_enough_information
      checkTUnspec(t());
      // jspecify_nullness_mismatch
      checkTUnionNull(t());

      // jspecify_nullness_not_enough_information
      checkNeverNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkT(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnionNull(tUnspec());

      // jspecify_nullness_mismatch
      checkNeverNull(tUnionNull());
      // jspecify_nullness_not_enough_information
      this.<T>checkUnspecNull(tUnionNull());
      // jspecify_nullness_mismatch
      checkT(tUnionNull());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnionNull());
      checkTUnionNull(tUnionNull());
    }
  }

  interface SuperNeverUnspec<T extends Object & @NullnessUnspecified Foo> extends SuperSuper<T> {
    default void x() {
      checkNeverNull(t());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(t());
      checkT(t());
      // jspecify_nullness_not_enough_information
      checkTUnspec(t());
      // jspecify_nullness_mismatch
      checkTUnionNull(t());

      // jspecify_nullness_not_enough_information
      checkNeverNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkT(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnionNull(tUnspec());

      // jspecify_nullness_mismatch
      checkNeverNull(tUnionNull());
      // jspecify_nullness_not_enough_information
      this.<T>checkUnspecNull(tUnionNull());
      // jspecify_nullness_mismatch
      checkT(tUnionNull());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnionNull());
      checkTUnionNull(tUnionNull());
    }
  }

  interface SuperNeverUnionNull<T extends Object & @Nullable Foo> extends SuperSuper<T> {
    default void x() {
      checkNeverNull(t());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(t());
      checkT(t());
      // jspecify_nullness_not_enough_information
      checkTUnspec(t());
      // jspecify_nullness_mismatch
      checkTUnionNull(t());

      // jspecify_nullness_not_enough_information
      checkNeverNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkT(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnionNull(tUnspec());

      // jspecify_nullness_mismatch
      checkNeverNull(tUnionNull());
      // jspecify_nullness_not_enough_information
      this.<T>checkUnspecNull(tUnionNull());
      // jspecify_nullness_mismatch
      checkT(tUnionNull());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnionNull());
      checkTUnionNull(tUnionNull());
    }
  }

  interface SuperUnspecNever<T extends @NullnessUnspecified Object & Foo> extends SuperSuper<T> {
    default void x() {
      checkNeverNull(t());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(t());
      checkT(t());
      // jspecify_nullness_not_enough_information
      checkTUnspec(t());
      // jspecify_nullness_mismatch
      checkTUnionNull(t());

      // jspecify_nullness_not_enough_information
      checkNeverNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkT(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnionNull(tUnspec());

      // jspecify_nullness_mismatch
      checkNeverNull(tUnionNull());
      // jspecify_nullness_not_enough_information
      this.<T>checkUnspecNull(tUnionNull());
      // jspecify_nullness_mismatch
      checkT(tUnionNull());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnionNull());
      checkTUnionNull(tUnionNull());
    }
  }

  interface SuperUnspecUnspec<T extends @NullnessUnspecified Object & @NullnessUnspecified Foo>
      extends SuperSuper<T> {
    // TODO(cpovirk): Add method calls like in the other classes.
  }

  interface SuperUnspecUnionNull<T extends @NullnessUnspecified Object & @Nullable Foo>
      extends SuperSuper<T> {
    // TODO(cpovirk): Add method calls like in the other classes.
  }

  interface SuperUnionNullNever<T extends @Nullable Object & Foo> extends SuperSuper<T> {
    default void x() {
      checkNeverNull(t());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(t());
      checkT(t());
      // jspecify_nullness_not_enough_information
      checkTUnspec(t());
      // jspecify_nullness_mismatch
      checkTUnionNull(t());

      // jspecify_nullness_not_enough_information
      checkNeverNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkT(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnionNull(tUnspec());

      // jspecify_nullness_mismatch
      checkNeverNull(tUnionNull());
      // jspecify_nullness_not_enough_information
      this.<T>checkUnspecNull(tUnionNull());
      // jspecify_nullness_mismatch
      checkT(tUnionNull());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnionNull());
      checkTUnionNull(tUnionNull());
    }
  }

  interface SuperUnionNullUnspec<T extends @Nullable Object & @NullnessUnspecified Foo>
      extends SuperSuper<T> {
    // TODO(cpovirk): Add method calls like in the other classes.
  }

  interface SuperUnionNullUnionNull<T extends @Nullable Object & @Nullable Foo>
      extends SuperSuper<T> {
    default void x() {
      // jspecify_nullness_mismatch
      checkNeverNull(t());
      // jspecify_nullness_mismatch
      checkUnspecNull(t());
      checkT(t());
      // jspecify_nullness_not_enough_information
      checkTUnspec(t());
      // jspecify_nullness_mismatch
      checkTUnionNull(t());

      // jspecify_nullness_mismatch
      checkNeverNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkUnspecNull(tUnspec());
      // jspecify_nullness_not_enough_information
      checkT(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnspec());
      // jspecify_nullness_not_enough_information
      checkTUnionNull(tUnspec());

      // jspecify_nullness_mismatch
      checkNeverNull(tUnionNull());
      // jspecify_nullness_not_enough_information
      this.<T>checkUnspecNull(tUnionNull());
      // jspecify_nullness_mismatch
      checkT(tUnionNull());
      // jspecify_nullness_not_enough_information
      checkTUnspec(tUnionNull());
      checkTUnionNull(tUnionNull());
    }
  }

  interface Foo {}

  interface Lib<T extends @Nullable Object> {}
}
