// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class ProjectViewUpdateCause : Comparable<ProjectViewUpdateCause> {
    companion object {
        fun plugin(pluginId: String): ProjectViewUpdateCause = ProjectView3rdPartyPluginUpdateCause(pluginId)

        @JvmField val UNKNOWN: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.UNKNOWN)<!>
        @JvmField val LEGACY: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.LEGACY)<!>
        @JvmField val SETTINGS: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.SETTINGS)<!>
        @JvmField val ACTION: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.ACTION)<!>
        @JvmField val BOOKMARKS: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.BOOKMARKS)<!>
        @JvmField val CLIPBOARD: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.CLIPBOARD)<!>
        @JvmField val EXTENSIONS_CHANGED: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.EXTENSIONS_CHANGED)<!>
        @JvmField val PSI_FLATTEN_PACKAGES: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PSI_FLATTEN_PACKAGES)<!>
        @JvmField val PSI_SCRATCH: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PSI_SCRATCH)<!>
        @JvmField val PSI_PROPERTY: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PSI_PROPERTY)<!>
        @JvmField val PSI_CHILDREN: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PSI_CHILDREN)<!>
        @JvmField val PSI_MOVE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PSI_MOVE)<!>
        @JvmField val ROOTS_LIBRARY: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.ROOTS_LIBRARY)<!>
        @JvmField val ROOTS_MODULE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.ROOTS_MODULE)<!>
        @JvmField val ROOTS_EP: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.ROOTS_EP)<!>
        @JvmField val FILE_OPENED: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.FILE_OPENED)<!>
        @JvmField val FILE_CLOSED: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.FILE_CLOSED)<!>
        @JvmField val FILE_STATUS: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.FILE_STATUS)<!>
        @JvmField val FILE_STATUSES: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.FILE_STATUSES)<!>
        @JvmField val FILE_APPEARANCE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.FILE_APPEARANCE)<!>
        @JvmField val VFS: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.VFS)<!>
        @JvmField val VFS_CREATE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.VFS_CREATE)<!>
        @JvmField val VFS_COPY: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.VFS_COPY)<!>
        @JvmField val VFS_MOVE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.VFS_MOVE)<!>
        @JvmField val VFS_DELETE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.VFS_DELETE)<!>
        @JvmField val PROBLEMS_APPEARED: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PROBLEMS_APPEARED)<!>
        @JvmField val PROBLEMS_DISAPPEARED: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PROBLEMS_DISAPPEARED)<!>
        @JvmField val SCOPE_CHOOSER: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.SCOPE_CHOOSER)<!>
        @JvmField val SCRATCHES: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.SCRATCHES)<!>
        @JvmField val REFACTORING: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.REFACTORING)<!>
        @JvmField val ANDROID: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.ANDROID)<!>
        @JvmField val PLUGIN_BAZEL: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_BAZEL)<!>
        @JvmField val PLUGIN_COVERAGE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_COVERAGE)<!>
        @JvmField val PLUGIN_DART: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_DART)<!>
        @JvmField val PLUGIN_DBE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_DBE)<!>
        @JvmField val PLUGIN_DTS: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_DTS)<!>
        @JvmField val PLUGIN_JAVAEE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_JAVAEE)<!>
        @JvmField val PLUGIN_JUPYTER: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_JUPYTER)<!>
        @JvmField val PLUGIN_PROJECT_FRAGMENTS: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_PROJECT_FRAGMENTS)<!>
        @JvmField val PLUGIN_PHP: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_PHP)<!>
        @JvmField val PLUGIN_PROPERTIES: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_PROPERTIES)<!>
        @JvmField val PLUGIN_PUPPET: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_PUPPET)<!>
        @JvmField val PLUGIN_PYTHON: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_PYTHON)<!>
        @JvmField val PLUGIN_REACT_BUDDY: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_REACT_BUDDY)<!>
        @JvmField val PLUGIN_RUBY: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_RUBY)<!>
        @JvmField val PLUGIN_SPRING: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_SPRING)<!>
        @JvmField val PLUGIN_WORKSPACE: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_WORKSPACE)<!>
        @JvmField val PLUGIN_XPATH: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.PLUGIN_XPATH)<!>
        @JvmField val DEBUG_VFS_INFO: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.DEBUG_VFS_INFO)<!>
        @JvmField
        val DEBUG_INDEXABILITY: ProjectViewUpdateCause = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>ProjectViewStandardUpdateCause(ProjectViewUpdateCauseId.DEBUG_INDEXABILITY)<!>
    }

    val id: ProjectViewUpdateCauseId
        get() = when (this) {
            is ProjectView3rdPartyPluginUpdateCause -> ProjectViewUpdateCauseId.PLUGIN_3RD_PARTY
            is ProjectViewStandardUpdateCause -> causeId
        }

    override fun compareTo(other: ProjectViewUpdateCause): Int {
        return when (this) {
            is ProjectViewStandardUpdateCause -> when (other) {
                is ProjectViewStandardUpdateCause -> causeId.compareTo(other.causeId)
                is ProjectView3rdPartyPluginUpdateCause -> 1
            }
            is ProjectView3rdPartyPluginUpdateCause -> when (other) {
                is ProjectView3rdPartyPluginUpdateCause -> pluginId.compareTo(other.pluginId)
                is ProjectViewStandardUpdateCause -> -1
            }
        }
    }
}<!>

enum class ProjectViewUpdateCauseId {
    PLUGIN_3RD_PARTY,

    UNKNOWN,
    LEGACY,
    SETTINGS,
    ACTION,
    BOOKMARKS,
    CLIPBOARD,
    EXTENSIONS_CHANGED,
    PSI_FLATTEN_PACKAGES,
    PSI_SCRATCH,
    PSI_PROPERTY,
    PSI_CHILDREN,
    PSI_MOVE,
    ROOTS_LIBRARY,
    ROOTS_MODULE,
    ROOTS_EP,
    FILE_OPENED,
    FILE_CLOSED,
    FILE_STATUS,
    FILE_STATUSES,
    FILE_APPEARANCE,
    VFS,
    VFS_CREATE,
    VFS_COPY,
    VFS_MOVE,
    VFS_DELETE,
    PROBLEMS_APPEARED,
    PROBLEMS_DISAPPEARED,
    SCOPE_CHOOSER,
    SCRATCHES,
    REFACTORING,
    ANDROID,
    PLUGIN_BAZEL,
    PLUGIN_COVERAGE,
    PLUGIN_DART,
    PLUGIN_DBE,
    PLUGIN_DTS,
    PLUGIN_JAVAEE,
    PLUGIN_JUPYTER,
    PLUGIN_PROJECT_FRAGMENTS,
    PLUGIN_PHP,
    PLUGIN_PROPERTIES,
    PLUGIN_PUPPET,
    PLUGIN_PYTHON,
    PLUGIN_REACT_BUDDY,
    PLUGIN_RUBY,
    PLUGIN_SPRING,
    PLUGIN_WORKSPACE,
    PLUGIN_XPATH,
    DEBUG_VFS_INFO,
    DEBUG_INDEXABILITY,
}


<!POSSIBLE_INITIALIZATION_DEADLOCK!>data class ProjectViewStandardUpdateCause(
    val causeId: ProjectViewUpdateCauseId
): ProjectViewUpdateCause() {
    init {
        require(causeId != ProjectViewUpdateCauseId.PLUGIN_3RD_PARTY)
    }
    override fun toString(): String = causeId.toString()
}<!>

data class ProjectView3rdPartyPluginUpdateCause(
    val pluginId: String
): ProjectViewUpdateCause() {
    override fun toString(): String = "PLUGIN=$pluginId"
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, getter, init, integerLiteral, isExpression, objectDeclaration, operator, override,
primaryConstructor, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
