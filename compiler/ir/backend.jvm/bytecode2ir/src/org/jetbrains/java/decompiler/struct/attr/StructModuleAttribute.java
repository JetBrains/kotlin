// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StructModuleAttribute extends StructGeneralAttribute {
  public String moduleName;
  public int moduleFlags;
  public String moduleVersion;

  public List<RequiresEntry> requires;
  public List<ExportsEntry> exports;
  public List<OpensEntry> opens;
  public List<String> uses;
  public List<ProvidesEntry> provides;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int moduleNameIndex = data.readUnsignedShort();
    this.moduleName = pool.getPrimitiveConstant(moduleNameIndex).getString();
    this.moduleFlags = data.readUnsignedShort();

    int moduleVersionIndex = data.readUnsignedShort();
    if (moduleVersionIndex != 0) {
      moduleVersion = pool.getPrimitiveConstant(moduleVersionIndex).getString();
    }

    this.requires = readRequires(data, pool);
    this.exports = readExports(data, pool);
    this.opens = readOpens(data, pool);
    this.uses = readUses(data, pool);
    this.provides = readProvides(data, pool);
  }

  public ModuleDescriptor asDescriptor() {
    var mods = EnumSet.noneOf(ModuleDescriptor.Modifier.class);
    if ((this.moduleFlags & CodeConstants.ACC_OPEN) != 0) mods.add(ModuleDescriptor.Modifier.OPEN);
    if ((this.moduleFlags & CodeConstants.ACC_SYNTHETIC) != 0) mods.add(ModuleDescriptor.Modifier.SYNTHETIC);
    if ((this.moduleFlags & CodeConstants.ACC_MANDATED) != 0) mods.add(ModuleDescriptor.Modifier.MANDATED);

    var builder = ModuleDescriptor.newModule(this.moduleName, mods);
    if (moduleVersion != null) builder.version(moduleVersion);

    for (final var requires : this.requires) {
      var rMods = EnumSet.noneOf(ModuleDescriptor.Requires.Modifier.class);
      if ((requires.flags & CodeConstants.ACC_TRANSITIVE) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
      if ((requires.flags & CodeConstants.ACC_STATIC_PHASE) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.STATIC);
      if ((requires.flags & CodeConstants.ACC_SYNTHETIC) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.SYNTHETIC);
      if ((requires.flags & CodeConstants.ACC_MANDATED) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.MANDATED);
      if (requires.moduleVersion != null) {
        builder.requires(rMods, requires.moduleName, ModuleDescriptor.Version.parse(requires.moduleVersion));
      } else {
        builder.requires(rMods, requires.moduleName);
      }
    }

    for (final var exports : this.exports) {
      var eMods = EnumSet.noneOf(ModuleDescriptor.Exports.Modifier.class);
      if ((exports.flags & CodeConstants.ACC_SYNTHETIC) != 0) eMods.add(ModuleDescriptor.Exports.Modifier.SYNTHETIC);
      if ((exports.flags & CodeConstants.ACC_MANDATED) != 0) eMods.add(ModuleDescriptor.Exports.Modifier.MANDATED);
      if (exports.exportToModules.isEmpty()) {
        builder.exports(eMods, exports.packageName.replace('/', '.'));
      } else {
        builder.exports(eMods, exports.packageName.replace('/', '.'), Set.copyOf(exports.exportToModules));
      }
    }

    for (final var opens : this.opens) {
      var oMods = EnumSet.noneOf(ModuleDescriptor.Opens.Modifier.class);
      if ((opens.flags & CodeConstants.ACC_SYNTHETIC) != 0) oMods.add(ModuleDescriptor.Opens.Modifier.SYNTHETIC);
      if ((opens.flags & CodeConstants.ACC_MANDATED) != 0) oMods.add(ModuleDescriptor.Opens.Modifier.MANDATED);

      if (opens.opensToModules.isEmpty()) {
        builder.opens(oMods, opens.packageName.replace('/', '.'));
      } else {
        builder.opens(oMods, opens.packageName.replace('/', '.'), Set.copyOf(opens.opensToModules));
      }
    }

    for (final var uses : this.uses) {
      builder.uses(uses.replace('/', '.'));
    }

    for (final var provides : this.provides) {
      builder.provides(
        provides.interfaceName.replace('/', '.'),
        provides.implementationNames.stream().map(name -> name.replace('/', '.')).collect(Collectors.toUnmodifiableList())
      );
    }

    return builder.build();
  }

  public List<RequiresEntry> readRequires(DataInputFullStream data, ConstantPool pool) throws IOException {
    int requiresCount = data.readUnsignedShort();
    if (requiresCount <= 0) return Collections.emptyList();

    List<RequiresEntry> requires = new ArrayList<>(requiresCount);
    for (int i = 0; i < requiresCount; i++) {
      int moduleNameIndex = data.readUnsignedShort();
      int requiresFlags = data.readUnsignedShort();
      int versionIndex = data.readUnsignedShort();
      String moduleName = pool.getPrimitiveConstant(moduleNameIndex).getString();
      String version = versionIndex == 0 ? null : pool.getPrimitiveConstant(versionIndex).getString();
      requires.add(new RequiresEntry(moduleName, requiresFlags, version));
    }
    return requires;
  }

  private static List<ExportsEntry> readExports(DataInputFullStream data, ConstantPool pool) throws IOException {
    int exportsCount = data.readUnsignedShort();
    if (exportsCount <= 0) return Collections.emptyList();

    List<ExportsEntry> exports = new ArrayList<>(exportsCount);
    for (int i = 0; i < exportsCount; i++) {
      int packageNameIndex = data.readUnsignedShort();
      int exportsFlags = data.readUnsignedShort();
      List<String> exportsToModules = readStringList(data, pool);
      String packageName = pool.getPrimitiveConstant(packageNameIndex).getString();
      exports.add(new ExportsEntry(packageName, exportsFlags, exportsToModules));
    }
    return exports;
  }

  private static List<OpensEntry> readOpens(DataInputFullStream data, ConstantPool pool) throws IOException {
    int opensCount = data.readUnsignedShort();
    if (opensCount <= 0) return Collections.emptyList();

    List<OpensEntry> opens = new ArrayList<>(opensCount);
    for (int i = 0; i < opensCount; i++) {
      int packageNameIndex = data.readUnsignedShort();
      int opensFlags = data.readUnsignedShort();
      List<String> opensToModules = readStringList(data, pool);
      String packageName = pool.getPrimitiveConstant(packageNameIndex).getString();
      opens.add(new OpensEntry(packageName, opensFlags, opensToModules));
    }
    return opens;
  }

  private static List<String> readUses(DataInputFullStream data, ConstantPool pool) throws IOException {
    return readStringList(data, pool);
  }

  private static List<ProvidesEntry> readProvides(DataInputFullStream data, ConstantPool pool) throws IOException {
    int providesCount = data.readUnsignedShort();
    if (providesCount <= 0) return Collections.emptyList();

    List<ProvidesEntry> provides = new ArrayList<>(providesCount);
    for (int i = 0; i < providesCount; i++) {
      int interfaceNameIndex = data.readUnsignedShort();
      String interfaceName = pool.getPrimitiveConstant(interfaceNameIndex).getString();
      List<String> implementationNames = readStringList(data, pool);
      provides.add(new ProvidesEntry(interfaceName, implementationNames));
    }
    return provides;
  }

  private static List<String> readStringList(DataInputFullStream data, ConstantPool pool) throws IOException {
    int count = data.readUnsignedShort();
    if (count <= 0) {
      return Collections.emptyList();
    }
    else {
      List<String> strings = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        int index = data.readUnsignedShort();
        strings.add(pool.getPrimitiveConstant(index).getString());
      }
      return strings;
    }
  }

  public static final class RequiresEntry {
    public final String moduleName;
    public final int flags;
    public final String moduleVersion;

    public RequiresEntry(String moduleName, int flags, String moduleVersion) {
      this.moduleName = moduleName;
      this.flags = flags;
      this.moduleVersion = moduleVersion;
    }
  }

  public static final class ExportsEntry {
    public final String packageName;
    public final int flags;
    public final List<String> exportToModules;

    public ExportsEntry(String packageName, int flags, List<String> exportToModules) {
      this.packageName = packageName;
      this.flags = flags;
      this.exportToModules = exportToModules;
    }
  }

  public static final class OpensEntry {
    public final String packageName;
    public final int flags;
    public final List<String> opensToModules;

    public OpensEntry(String packageName, int flags, List<String> exportToModules) {
      this.packageName = packageName;
      this.flags = flags;
      this.opensToModules = exportToModules;
    }
  }

  public static final class ProvidesEntry {
    public final String interfaceName;
    public final List<String> implementationNames;

    public ProvidesEntry(String interfaceName, List<String> implementationNames) {
      this.interfaceName = interfaceName;
      this.implementationNames = implementationNames;
    }
  }
}