package test

class ClassA {
    class classB {
        fun memberFromB(): Int = 100

        class BC {
            val memberFromBB: Int = 150
        }

        object BO {
            val memberFromBO: Int = 175
        }
    }

    inner class classC {
        val memberFromC: Int = 200

        class CC {
            val memberFromCC: Int = 250
        }

        object CO {
            val memberFromCO: Int = 265
        }
    }

    companion object {
        val stat: Int

        class D {
            val memberFromD: Int = 275
        }
    }

    object ObjA {
        val memberFromObjA: Int = 300
    }
}

