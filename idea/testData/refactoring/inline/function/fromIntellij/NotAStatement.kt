class AAA {
    fun fff(myProject: Project) {
        ensureFilesWritable(myProject, *Array(1) { "2" })
    }

    private fun ensure<caret>FilesWritable(project: Project, vararg strings: String): Boolean {
        return !ensureFilesWritable(strings).hasReadonlyFiles()
    }

    private fun ensureFilesWritable(strings: Array<out String>): Status {
        return Status(strings)
    }

    inner class Status(strings: Array<out String>) {

        fun hasReadonlyFiles(): Boolean {
            return true
        }
    }

    inner class Project
}
