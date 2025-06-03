package org.jetbrains.kotlin.buildtools.api.v2

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField

public open class CommonKlibBasedArguments : CommonCompilerArguments() {
  private val optionsMap: MutableMap<CommonKlibBasedArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
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
    public val XKLIB_RELATIVE_PATH_BASE: CommonKlibBasedArgument<Array<String>?> =
        CommonKlibBasedArgument("XKLIB_RELATIVE_PATH_BASE")

    /**
     * Normalize absolute paths in klibs.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XKLIB_NORMALIZE_ABSOLUTE_PATH: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("XKLIB_NORMALIZE_ABSOLUTE_PATH")

    /**
     * Enable signature uniqueness checks.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XKLIB_ENABLE_SIGNATURE_CLASH_CHECKS: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("XKLIB_ENABLE_SIGNATURE_CLASH_CHECKS")

    /**
     * Use partial linkage mode.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPARTIAL_LINKAGE: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("XPARTIAL_LINKAGE")

    /**
     * Define the compile-time log level for partial linkage.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPARTIAL_LINKAGE_LOGLEVEL: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("XPARTIAL_LINKAGE_LOGLEVEL")

    /**
     * Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XKLIB_DUPLICATED_UNIQUE_NAME_STRATEGY: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("XKLIB_DUPLICATED_UNIQUE_NAME_STRATEGY")

    /**
     * Enable experimental support to invoke IR Inliner before Klib serialization.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XKLIB_IR_INLINER: CommonKlibBasedArgument<Boolean> =
        CommonKlibBasedArgument("XKLIB_IR_INLINER")

    /**
     * Specify the custom ABI version to be written in KLIB. This option is intended only for tests.
     * Warning: This option does not affect KLIB ABI. Neither allows it making a KLIB backward-compatible with older ABI versions.
     * The only observable effect is that a custom ABI version is written to KLIB manifest file.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XKLIB_ABI_VERSION: CommonKlibBasedArgument<String?> =
        CommonKlibBasedArgument("XKLIB_ABI_VERSION")
  }
}
