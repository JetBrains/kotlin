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
class AnnotatedInnerOfNonParameterized {
  interface Lib<T extends @Nullable Object> {}

  class Nested {
    class DoublyNested {}
  }

  @Nullable Nested x4;

  // jspecify_nullness_intrinsically_not_nullable
  @Nullable AnnotatedInnerOfNonParameterized.Nested x5;

  AnnotatedInnerOfNonParameterized.@Nullable Nested x6;

  // jspecify_nullness_intrinsically_not_nullable
  @Nullable AnnotatedInnerOfNonParameterized.Nested.DoublyNested x7;

  // jspecify_nullness_intrinsically_not_nullable
  AnnotatedInnerOfNonParameterized.@Nullable Nested.DoublyNested x8;

  AnnotatedInnerOfNonParameterized.Nested.@Nullable DoublyNested x9;

  // jspecify_nullness_intrinsically_not_nullable
  Lib<@Nullable AnnotatedInnerOfNonParameterized.Nested.DoublyNested> l1;

  // jspecify_nullness_intrinsically_not_nullable
  Lib<AnnotatedInnerOfNonParameterized.@Nullable Nested.DoublyNested> l2;

  Lib<AnnotatedInnerOfNonParameterized.Nested.DoublyNested> l3;
}
