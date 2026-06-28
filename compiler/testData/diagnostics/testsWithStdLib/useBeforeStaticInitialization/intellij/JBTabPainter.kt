// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>interface JBTabPainter {
    companion object {
        @JvmStatic
        val DEFAULT: JBTabPainter = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>JBDefaultTabPainter()<!>

        @JvmStatic
        val EDITOR: JBEditorTabPainter = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS, CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>JBEditorTabPainter()<!>

        @JvmStatic
        val TOOL_WINDOW: JBTabPainter = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS, CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ToolWindowTabPainter()<!>

        @JvmStatic
        val DEBUGGER: JBTabPainter = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>JBDefaultTabPainter(DebuggerTabTheme())<!>
    }

    fun getBackgroundColor(): String = "#000000"
}<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>open class JBDefaultTabPainter(val theme : TabTheme = DefaultTabTheme()) : JBTabPainter<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class JBEditorTabPainter : JBDefaultTabPainter(EditorTabTheme())<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>internal class ToolWindowTabPainter: JBDefaultTabPainter(ToolWindowTabTheme())<!>

interface TabTheme {
    val background: String get() = "#000000"
}

open class DefaultTabTheme : TabTheme

class EditorTabTheme : TabTheme

internal class ToolWindowTabTheme : DefaultTabTheme()

internal class DebuggerTabTheme : DefaultTabTheme()

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, interfaceDeclaration,
objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
