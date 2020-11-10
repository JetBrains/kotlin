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

@DefaultNonNull
class AnnotatedInnerOfParameterized<T> {
  interface Lib<T extends @Nullable Object> {}

  class Nested {
    class DoublyNested {}
  }

  @Nullable Nested x4;

  // jspecify_nullness_intrinsically_not_nullable
  @Nullable AnnotatedInnerOfParameterized<?>.Nested x5;

  AnnotatedInnerOfParameterized<?>.@Nullable Nested x6;

  // jspecify_nullness_intrinsically_not_nullable
  @Nullable AnnotatedInnerOfParameterized<?>.Nested.DoublyNested x7;

  // jspecify_nullness_intrinsically_not_nullable
  AnnotatedInnerOfParameterized<?>.@Nullable Nested.DoublyNested x8;

  AnnotatedInnerOfParameterized<?>.Nested.@Nullable DoublyNested x9;

  // jspecify_nullness_intrinsically_not_nullable
  Lib<@Nullable AnnotatedInnerOfParameterized<?>.Nested.DoublyNested> l1;

  // jspecify_nullness_intrinsically_not_nullable
  Lib<AnnotatedInnerOfParameterized<?>.@Nullable Nested.DoublyNested> l2;

  Lib<AnnotatedInnerOfParameterized<?>.Nested.DoublyNested> l3;

  void takeNotNull(Object o) { }
  void takeLibNotNull(Lib<Object> l) { }
}

static class Checker {
  void main(AnnotatedInnerOfParameterized<?> o) {
    // jspecify_nullness_mismatch
    o.takeNotNull(o.x4);
    o.takeNotNull(o.x5);
    // jspecify_nullness_mismatch
    o.takeNotNull(o.x6);
    o.takeNotNull(o.x7);
    o.takeNotNull(o.x8);
    // jspecify_nullness_mismatch
    o.takeNotNull(o.x9);

    o.takeLibNotNull(o.l1);
    o.takeLibNotNull(o.l2);
    o.takeLibNotNull(o.l3);
  }
}
