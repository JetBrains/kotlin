// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR

// FILE: data/PodcastWithExtraInfo.kt
package data

class PodcastWithExtraInfo {
    lateinit var url: String
    var lastEpisodeDate: kotlin.time.TimeSource? = null
    operator fun component1() = url
    operator fun component2() = lastEpisodeDate
}

// FILE: main.kt
package home

import data.PodcastWithExtraInfo

fun preview(featuredPodcast: PodcastWithExtraInfo) {
    val (podcast, lastEpisodeDate) = featuredPodcast
}