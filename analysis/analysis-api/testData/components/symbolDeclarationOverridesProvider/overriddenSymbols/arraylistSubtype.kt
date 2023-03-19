class MyList : ArrayList<Int>() {
    override val size<caret>: Int = 0
    override fun get(index: Int): Int = 0
}

// RESULT
// ALL:
// java/util/ArrayList.size: Int
// java/util/AbstractList.size: Int
// java/util/AbstractCollection.size: Int
// List.size: Int
// Collection.size: Int

// DIRECT:
// java/util/ArrayList.size: Int
