// FIR_IDENTICAL

// FILE: TableView.java

import java.util.Collection;

public class TableView<Item> {
    public Collection<Item> getSelection() { return null; }
    public void setSelection(Collection<Item> selection) {}
}

// FILE: JavaTableView.java

import java.util.List;

public class JavaTableView<Item> extends TableView<Item> {
    @Override public List<Item> getSelection() { return null; }
}

// FILE: main.kt

class KotlinTableView<Item>: TableView<Item>() {
    override fun getSelection(): List<Item>? { return null }
}

fun foo(
    javaTable: JavaTableView<String>,
    kotlinTable: KotlinTableView<String>,
    selection: ArrayList<String>
) {
    javaTable.selection = selection
    kotlinTable.selection = selection
}
