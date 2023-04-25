open class OpenClassRemovedTAImpl(x: Int) : OpenClassRemovedTA(-x)
data class OpenClassRemovedTATypeParameterHolder<T : OpenClassRemovedTA>(val t: T)
data class OpenClassRemovedTAImplTypeParameterHolder<T : OpenClassRemovedTAImpl>(val t: T)

fun getOpenClassRemovedTA(x: Int): OpenClassRemovedTA = OpenClassRemovedTA(x)
fun setOpenClassRemovedTA(value: OpenClassRemovedTA?): String = value?.toString() ?: "setOpenClassRemovedTA"
fun getOpenClassRemovedTAImpl(x: Int): OpenClassRemovedTA = OpenClassRemovedTAImpl(x)
fun setOpenClassRemovedTAImpl(value: OpenClassRemovedTAImpl?): String = value?.toString() ?: "setOpenClassRemovedTAImpl"

fun getOpenClassRemovedTATypeParameterHolder1(x: Int): OpenClassRemovedTATypeParameterHolder<OpenClassRemovedTA> = OpenClassRemovedTATypeParameterHolder(OpenClassRemovedTA(x))
fun getOpenClassRemovedTATypeParameterHolder2(x: Int): OpenClassRemovedTATypeParameterHolder<OpenClassRemovedTAImpl> = OpenClassRemovedTATypeParameterHolder(OpenClassRemovedTAImpl(x))
fun setOpenClassRemovedTATypeParameterHolder1(value: OpenClassRemovedTATypeParameterHolder<OpenClassRemovedTA>?): String = value?.toString() ?: "setOpenClassRemovedTATypeParameterHolder1"
fun setOpenClassRemovedTATypeParameterHolder2(value: OpenClassRemovedTATypeParameterHolder<OpenClassRemovedTAImpl>?): String = value?.toString() ?: "setOpenClassRemovedTATypeParameterHolder2"

fun getOpenClassRemovedTAImplTypeParameterHolder(x: Int): OpenClassRemovedTAImplTypeParameterHolder<OpenClassRemovedTAImpl> = OpenClassRemovedTAImplTypeParameterHolder(OpenClassRemovedTAImpl(x))
fun setOpenClassRemovedTAImplTypeParameterHolder(value: OpenClassRemovedTAImplTypeParameterHolder<OpenClassRemovedTAImpl>?): String = value?.toString() ?: "setOpenClassRemovedTAImplTypeParameterHolder"

open class OpenClassChangedTAImpl(x: Int) : OpenClassChangedTA(-x)
data class OpenClassChangedTATypeParameterHolder<T : OpenClassChangedTA>(val t: T)
data class OpenClassChangedTAImplTypeParameterHolder<T : OpenClassChangedTAImpl>(val t: T)

fun getOpenClassChangedTA(x: Int): OpenClassChangedTA = OpenClassChangedTA(x)
fun setOpenClassChangedTA(value: OpenClassChangedTA?): String = value?.toString() ?: "setOpenClassChangedTA"
fun getOpenClassChangedTAImpl(x: Int): OpenClassChangedTA = OpenClassChangedTAImpl(x)
fun setOpenClassChangedTAImpl(value: OpenClassChangedTAImpl?): String = value?.toString() ?: "setOpenClassChangedTAImpl"

fun getOpenClassChangedTATypeParameterHolder1(x: Int): OpenClassChangedTATypeParameterHolder<OpenClassChangedTA> = OpenClassChangedTATypeParameterHolder(OpenClassChangedTA(x))
fun getOpenClassChangedTATypeParameterHolder2(x: Int): OpenClassChangedTATypeParameterHolder<OpenClassChangedTAImpl> = OpenClassChangedTATypeParameterHolder(OpenClassChangedTAImpl(x))
fun setOpenClassChangedTATypeParameterHolder1(value: OpenClassChangedTATypeParameterHolder<OpenClassChangedTA>?): String = value?.toString() ?: "setOpenClassChangedTATypeParameterHolder1"
fun setOpenClassChangedTATypeParameterHolder2(value: OpenClassChangedTATypeParameterHolder<OpenClassChangedTAImpl>?): String = value?.toString() ?: "setOpenClassChangedTATypeParameterHolder2"

fun getOpenClassChangedTAImplTypeParameterHolder(x: Int): OpenClassChangedTAImplTypeParameterHolder<OpenClassChangedTAImpl> = OpenClassChangedTAImplTypeParameterHolder(OpenClassChangedTAImpl(x))
fun setOpenClassChangedTAImplTypeParameterHolder(value: OpenClassChangedTAImplTypeParameterHolder<OpenClassChangedTAImpl>?): String = value?.toString() ?: "setOpenClassChangedTAImplTypeParameterHolder"

open class OpenClassNarrowedVisibilityTAImpl(x: Int) : OpenClassNarrowedVisibilityTA(-x)
data class OpenClassNarrowedVisibilityTATypeParameterHolder<T : OpenClassNarrowedVisibilityTA>(val t: T)
data class OpenClassNarrowedVisibilityTAImplTypeParameterHolder<T : OpenClassNarrowedVisibilityTAImpl>(val t: T)

fun getOpenClassNarrowedVisibilityTA(x: Int): OpenClassNarrowedVisibilityTA = OpenClassNarrowedVisibilityTA(x)
fun setOpenClassNarrowedVisibilityTA(value: OpenClassNarrowedVisibilityTA?): String = value?.toString() ?: "setOpenClassNarrowedVisibilityTA"
fun getOpenClassNarrowedVisibilityTAImpl(x: Int): OpenClassNarrowedVisibilityTA = OpenClassNarrowedVisibilityTAImpl(x)
fun setOpenClassNarrowedVisibilityTAImpl(value: OpenClassNarrowedVisibilityTAImpl?): String = value?.toString() ?: "setOpenClassNarrowedVisibilityTAImpl"

fun getOpenClassNarrowedVisibilityTATypeParameterHolder1(x: Int): OpenClassNarrowedVisibilityTATypeParameterHolder<OpenClassNarrowedVisibilityTA> = OpenClassNarrowedVisibilityTATypeParameterHolder(OpenClassNarrowedVisibilityTA(x))
fun getOpenClassNarrowedVisibilityTATypeParameterHolder2(x: Int): OpenClassNarrowedVisibilityTATypeParameterHolder<OpenClassNarrowedVisibilityTAImpl> = OpenClassNarrowedVisibilityTATypeParameterHolder(OpenClassNarrowedVisibilityTAImpl(x))
fun setOpenClassNarrowedVisibilityTATypeParameterHolder1(value: OpenClassNarrowedVisibilityTATypeParameterHolder<OpenClassNarrowedVisibilityTA>?): String = value?.toString() ?: "setOpenClassNarrowedVisibilityTATypeParameterHolder1"
fun setOpenClassNarrowedVisibilityTATypeParameterHolder2(value: OpenClassNarrowedVisibilityTATypeParameterHolder<OpenClassNarrowedVisibilityTAImpl>?): String = value?.toString() ?: "setOpenClassNarrowedVisibilityTATypeParameterHolder2"

fun getOpenClassNarrowedVisibilityTAImplTypeParameterHolder(x: Int): OpenClassNarrowedVisibilityTAImplTypeParameterHolder<OpenClassNarrowedVisibilityTAImpl> = OpenClassNarrowedVisibilityTAImplTypeParameterHolder(OpenClassNarrowedVisibilityTAImpl(x))
fun setOpenClassNarrowedVisibilityTAImplTypeParameterHolder(value: OpenClassNarrowedVisibilityTAImplTypeParameterHolder<OpenClassNarrowedVisibilityTAImpl>?): String = value?.toString() ?: "setOpenClassNarrowedVisibilityTAImplTypeParameterHolder"
