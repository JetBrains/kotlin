import java.util.Collections

fun <T> checkSubtype(t: T) = t/* DeclarationStructureElement */

val ab = checkSubtype<List<Int>?>(Collections.emptyList<Int>())/* DeclarationStructureElement */
