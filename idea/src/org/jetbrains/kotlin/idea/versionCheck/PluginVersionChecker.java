package org.jetbrains.kotlin.idea.versionCheck;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.Nullable;

public interface PluginVersionChecker {

  ExtensionPointName<PluginVersionChecker> EP_NAME = new ExtensionPointName<>("org.jetbrains.kotlin.pluginVersionChecker");

  /** Returns the latest Kotlin plugin version for the current build number, os, and channel.
   *
   * @param host plugin download host, null means the default host
   * @return a nonnull plugin descriptor if there is a newer version to advertise, and null otherwise
   * @throws PluginVersionCheckFailed if check fails
   */
  IdeaPluginDescriptor getLatest(@Nullable String currentVersion, @Nullable String host) throws PluginVersionCheckFailed;
}
