package one.two

interface BaseInterface

enum class EnumEntries {
    FirstEntry,
    SecondEntryWithBody {
        fun foo() {

        }
    }
}

enum class EnumClassWithInterface : BaseInterface {
    NewFirstEntry,
    NewSecondEntryWithBody {
        fun foo() {

        }
    }
}
