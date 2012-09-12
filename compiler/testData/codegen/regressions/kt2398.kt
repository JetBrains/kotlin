class C {
    public object Obj {
        val o = "O"

        object InnerObj {
          fun k() = "K"
        }

        class D {
            val ko = "KO"
        }
    }
}

fun box() = C().Obj.o + C().Obj.InnerObj.k()  + C().Obj.D().ko