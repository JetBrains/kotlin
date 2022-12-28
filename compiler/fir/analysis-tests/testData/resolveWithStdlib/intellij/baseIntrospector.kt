// MODULE: m1
// FILE: m1.kt

package m1

interface BasicDatabase
interface BasicSchema

abstract class BaseIntrospector<D : BasicDatabase, S : BasicSchema> {
    protected abstract fun createDatabaseRetriever(database: D): AbstractDatabaseRetriever<out D>

    protected abstract inner class AbstractDatabaseRetriever<D : BasicDatabase>
    protected constructor(protected val database: D)
        : AbstractRetriever()

    protected abstract inner class AbstractRetriever
}

interface BasicSingleDatabase : BasicDatabase
interface BasicModSchema : BasicSchema

abstract class BaseSingleDatabaseIntrospector<D : BasicSingleDatabase, S : BasicModSchema>
protected constructor() : BaseIntrospector<D, S>()

// MODULE: m2(m1)
// FILE: m2.kt

package m2

import m1.*

interface SqliteRoot : BasicSingleDatabase
interface SqliteSchema : BasicModSchema

class SqliteIntrospector : BaseSingleDatabaseIntrospector<SqliteRoot, SqliteSchema>() {
    override fun createDatabaseRetriever(database: SqliteRoot) =
        object : AbstractDatabaseRetriever<SqliteRoot>(database) {
        }
}
