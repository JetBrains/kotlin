import java.util.Collections

fun <T> checkSubtype(t: T) = t/* NonReanalyzableNonClassDeclarationStructureElement */

val ab = checkSubtype<List<Int>?>(Collections.emptyList<Int>())/* NonReanalyzableNonClassDeclarationStructureElement */
