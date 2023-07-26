interface A<T>/* NonReanalyzableClassDeclarationStructureElement */

typealias AS = A<String>/* NonReanalyzableNonClassDeclarationStructureElement */

class C : AS {/* NonReanalyzableClassDeclarationStructureElement */
    constructor()/* NonReanalyzableNonClassDeclarationStructureElement */
}
