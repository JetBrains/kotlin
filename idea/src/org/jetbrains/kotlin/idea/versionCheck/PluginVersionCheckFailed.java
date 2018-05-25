package org.jetbrains.kotlin.idea.versionCheck;

public class PluginVersionCheckFailed extends Exception {
  public PluginVersionCheckFailed(String message) {
    super(message);
  }
}
