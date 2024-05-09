// WITH_STDLIB
// FILE: Cursor.java

public interface Cursor {
    boolean moveToNext();
}

// FILE: main.kt

class PartData

private fun retrievePartData(): PartData? = null

private fun queryMmsPartTable(cursor: Cursor) =
    buildList {
        while (cursor.moveToNext()) {
            retrievePartData()
                ?.let(<expr>::add</expr>)
        }
    }
