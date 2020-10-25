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
interface CovariantReturns {
  @Nullable
  Object makeObject();

  Lib<?> makeImplicitlyObjectBounded();

  Lib<? extends @Nullable Object> makeExplicitlyObjectBounded();

  interface Subtype extends CovariantReturns {
    @Override
    Object makeObject();

    @Override
    Lib<? extends Object> makeImplicitlyObjectBounded();

    @Override
    Lib<? extends Object> makeExplicitlyObjectBounded();
  }

  default void go(Subtype s) {
    checkObject(s.makeObject());
    checkLibOfObject(s.makeImplicitlyObjectBounded());
    checkLibOfObject(s.makeExplicitlyObjectBounded());
  }

  void checkObject(Object o);

  void checkLibOfObject(Lib<? extends Object> o);

  interface Lib<T extends @Nullable Object> {}
}
