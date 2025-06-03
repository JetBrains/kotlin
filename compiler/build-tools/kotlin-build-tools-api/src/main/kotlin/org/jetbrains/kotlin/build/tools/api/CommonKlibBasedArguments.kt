package org.jetbrains.kotlin.build.tools.api

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField

public open class CommonKlibBasedArguments : CommonCompilerArguments() {
  private val optionsMap: MutableMap<CommonKlibBasedArgument<*>, Any?> = mutableMapOf()

  public operator fun <V> `get`(key: CommonKlibBasedArgument<V>): V? = optionsMap[key] as V?

  public operator fun <V> `set`(key: CommonKlibBasedArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  public class CommonKlibBasedArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Provide a base path to compute the source's relative paths in klib (default is empty).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val KLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgument<Array<String>?> =
        CommonKlibBasedArgument("KLIB_RELATIVE_PATH_BASE")

    /**
     * Normalize absolute paths in klibs.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val KLIB_NORMALIZE_ABSOLUTE_PATH: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("KLIB_NORMALIZE_ABSOLUTE_PATH")

    /**
     * Enable signature uniqueness checks.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val KLIB_ENABLE_SIGNATURE_CLASH_CHECKS: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("KLIB_ENABLE_SIGNATURE_CLASH_CHECKS")

    /**
     * Use partial linkage mode.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PARTIAL_LINKAGE: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("PARTIAL_LINKAGE")

    /**
     * Define the compile-time log level for partial linkage.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val PARTIAL_LINKAGE_LOGLEVEL: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("PARTIAL_LINKAGE_LOGLEVEL")

    /**
     * Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY")

    /**
     * Enable experimental support to invoke IR Inliner before Klib serialization.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val KLIB_IR_INLINER: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("KLIB_IR_INLINER")

    /**
     * Specify the custom ABI version to be written in KLIB. This option is intended only for tests.
     * Warning: This option does not affect KLIB ABI. Neither allows it making a KLIB backward-compatible with older ABI versions.
     * The only observable effect is that a custom ABI version is written to KLIB manifest file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val KLIB_ABI_VERSION: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("KLIB_ABI_VERSION")
  }
}
