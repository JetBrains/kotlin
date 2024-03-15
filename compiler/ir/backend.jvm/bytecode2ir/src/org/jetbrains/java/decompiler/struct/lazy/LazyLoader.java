// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.lazy;

/*import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;*/

public class LazyLoader {
  /*private final Map<String, Link> mapClassLinks = new HashMap<>();
  private final IBytecodeProvider provider;

  public LazyLoader(IBytecodeProvider provider) {
    this.provider = provider;
  }

  public void addClassLink(String className, Link link) {
    mapClassLinks.put(className, link);
  }

  public void removeClassLink(String className) {
    mapClassLinks.remove(className);
  }

  public Link getClassLink(String className) {
    return mapClassLinks.get(className);
  }

  public ConstantPool loadPool(String className) {
    try (DataInputFullStream in = getClassStream(className)) {
      if (in != null) {
        in.discard(8);
        return new ConstantPool(in);
      }

      return null;
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public byte[] getClassBytes(String externalPath, String internalPath) throws IOException {
    return provider.getBytecode(externalPath, internalPath);
  }

  public DataInputFullStream getClassStream(String externalPath, String internalPath) throws IOException {
    return new DataInputFullStream(getClassBytes(externalPath, internalPath));
  }

  public DataInputFullStream getClassStream(String qualifiedClassName) throws IOException {
    Link link = mapClassLinks.get(qualifiedClassName);
    return link == null ? null : link.data != null ? new DataInputFullStream(link.data) : getClassStream(link.externalPath, link.internalPath);
  }

  public static class Link {
    public final String externalPath;
    public final String internalPath;
    public final byte[] data;

    public Link(String externalPath, String internalPath) {
        this(externalPath, internalPath, null);
    }

    public Link(String externalPath, String internalPath, byte[] data) {
      this.externalPath = externalPath;
      this.internalPath = internalPath;
      this.data = data;
    }
  }*/
}
