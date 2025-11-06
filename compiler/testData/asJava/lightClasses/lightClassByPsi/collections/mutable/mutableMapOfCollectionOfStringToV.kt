// WITH_STDLIB
package test

abstract class SMutableMap<VElem> : MutableMap<Collection<String>, VElem>

//abstract class SMutableMap2<VElem> : MutableMap<Collection<String>, VElem> by mutableMapOf()
//
//open class SMutableMap3<VElem> : MutableMap<Collection<String>, VElem> {
//    override val entries: MutableSet<MutableMap.MutableEntry<Collection<String>, VElem>>
//        get() = TODO("Not yet implemented")
//    override val keys: MutableSet<Collection<String>>
//        get() = TODO("Not yet implemented")
//    override val values: MutableCollection<VElem>
//        get() = TODO("Not yet implemented")
//
//    override fun clear() {
//        TODO("Not yet implemented")
//    }
//
//    override fun put(key: Collection<String>, value: VElem): VElem? {
//        TODO("Not yet implemented")
//    }
//
//    override fun putAll(from: Map<out Collection<String>, VElem>) {
//        TODO("Not yet implemented")
//    }
//
//    override fun remove(key: Collection<String>): VElem? {
//        TODO("Not yet implemented")
//    }
//
//    override val size: Int
//        get() = TODO("Not yet implemented")
//
//    override fun containsKey(key: Collection<String>): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun containsValue(value: VElem): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun get(key: Collection<String>): VElem? {
//        TODO("Not yet implemented")
//    }
//
//    override fun isEmpty(): Boolean {
//        TODO("Not yet implemented")
//    }
//}
