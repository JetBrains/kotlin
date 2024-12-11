// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

//import org.jetbrains.annotations.ApiStatus;
//import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;

//@Internal
public interface StubTreeSerializer {
  void serialize(@NotNull Stub rootStub, @NotNull OutputStream stream);

  @NotNull Stub deserialize(@NotNull InputStream stream) throws SerializerNotFoundException;
}
