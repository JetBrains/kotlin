// LANGUAGE: +CompanionBlocksAndExtensions

class Container {
    interface Vector<T> {
        companion object { // companion object
            val ZeroObject get() = 0
        }
        companion { // companion block
            val ZeroBlock get() = 0
        }
    }
}

class VectorImpl<T>: Container.Vector<T>

// companion object extension
val Container.Vector.Companion.UnitXObjectExtension get() = 0

// companion extension
companion val Container.Vector.UnitXExtension get() = 0

/**
 * [Container.Vector.ZeroO<caret_1>bject]
 * [Container.Vector.Companion.ZeroO<caret_2>bject]
 * [Container.Vector.ZeroBl<caret_3>ock]
 * [Container.Vector.Companion.Zer<caret_4>oBlock]
 * [Container.Vector.UnitXObjectExte<caret_5>nsion]
 * [Container.Vector.UnitXExten<caret_6>sion]
 * [Container.Vector.Companion.UnitXObj<caret_7>ectExtension]
 * [Container.Vector.Companion.UnitXExtens<caret_8>ion]
 *
 * Must all be unresolved:
 * [Container.ZeroO<caret_9>bject]
 * [Container.Companion.ZeroO<caret_10>bject]
 * [Container.ZeroBl<caret_11>ock]
 * [Container.Companion.Zer<caret_12>oBlock]
 * [Container.UnitXObjectExte<caret_13>nsion]
 * [Container.UnitXExten<caret_14>sion]
 * [Container.Companion.UnitXObj<caret_15>ectExtension]
 * [Container.Companion.UnitXExtens<caret_16>ion]
 *
 * [VectorImpl.ZeroO<caret_17>bject]
 * [VectorImpl.Companion.ZeroO<caret_18>bject]
 * [VectorImpl.ZeroBl<caret_19>ock]
 * [VectorImpl.Companion.Zer<caret_20>oBlock]
 * [VectorImpl.UnitXObjectExte<caret_21>nsion]
 * [VectorImpl.UnitXExten<caret_22>sion]
 * [VectorImpl.Companion.UnitXObj<caret_23>ectExtension]
 * [VectorImpl.Companion.UnitXExtens<caret_24>ion]
 */
fun usage() {}
