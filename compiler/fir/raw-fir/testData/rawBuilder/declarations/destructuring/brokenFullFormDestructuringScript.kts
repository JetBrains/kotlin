// IGNORE_TREE_ACCESS: KT-89821
// IGNORE_BODY_CALCULATOR: the LL rebuild of the nameless second entry takes the destructuring kind from the entry itself

class Tuple(val first: String, val second: Int)

(val a = first, second) = x
