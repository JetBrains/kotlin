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
class AnnotatedWildcard {
  interface Lib<T extends @Nullable Object> {}

  // jspecify_unrecognized_location
  Lib<@Nullable ?> x1;

  // jspecify_unrecognized_location
  Lib<@Nullable ? extends Object> x2;

  // jspecify_unrecognized_location
  Lib<@Nullable ? super Object> x3;

  // jspecify_unrecognized_location
  Lib<@Nullable ? extends @Nullable Object> x4;

  // jspecify_unrecognized_location
  Lib<@Nullable ? super @Nullable Object> x5;

  void takeLibExtendsNotNull(Lib<? extends Object> l) { }

  void takeLibSuperNullable(Lib<? super @Nullable Object> l) { }
}

static class Checker {
  void main(AnnotatedWildcard o) {
    o.takeLibExtendsNotNull(o.x1);
    o.takeLibExtendsNotNull(o.x2);
    // jspecify_nullness_mismatch
    o.takeLibSuperNullable(o.x3);
    // jspecify_nullness_mismatch
    o.takeLibExtendsNotNull(o.x4);
    o.takeLibSuperNullable(o.x5);
  }
}
