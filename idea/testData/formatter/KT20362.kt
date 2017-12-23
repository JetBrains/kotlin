val ret = (DB.Article innerJoin DB.Project).
    slice(DB.Article.project, DB.Article.project.count()).
    select {
        filterBlogs(params)
    }.groupBy(DB.Article.project).map {
    val count = it[DB.Article.project.count()]
    val project = it[DB.Article.team.count()]
}