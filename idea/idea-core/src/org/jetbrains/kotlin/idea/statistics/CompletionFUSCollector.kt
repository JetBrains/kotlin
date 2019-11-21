/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

object CompletionFUSCollector {
    private const val WindowPopulationTimeAttribute = "window_population_time"
    private const val WindowAppearanceTimeAttribute = "window_appearance_time"
    private const val CompletionTypeAttribute = "completion_type"
    private const val FileTypeAttribute = "file_type"
    private const val FinishReason = "finish_reason"
    private const val ChoiceAtPositionAttribute = "choice_at_position"
    private const val InvocationCount = "invocation_count"
    private const val CompletionEventAttribute = "completion_event"

    private const val Chosen = "chosen"
    private const val NotChosen = "not_chosen"

    private const val EventName = "Completion"

    @Volatile
    var completionStatsData: CompletionStatsData? = null

    data class CompletionStatsData(
        val startTime: Long,
        val shownTime: Long? = null,
        val finishTime: Long? = null,
        val completionType: CompletionTypeStats? = null,
        val fileType: FileTypeStats? = null,
        val finishReason: FinishReasonStats? = null,
        val selectedItem: Int? = null,
        val invocationCount: Int? = null
    )

    fun log(completionStatsData: CompletionStatsData?) {
        if (completionStatsData?.fileType == null) return

        val data = mutableMapOf<String, String>()
        data[FileTypeAttribute] = completionStatsData.fileType.toString()
        data[CompletionTypeAttribute] = completionStatsData.completionType.toString()

        if (completionStatsData.finishTime != null) {
            val populationTime = (completionStatsData.finishTime - completionStatsData.startTime).toString()
            data[WindowPopulationTimeAttribute] = populationTime
        }

        if (completionStatsData.shownTime != null) {
            val appearanceTime = (completionStatsData.shownTime - completionStatsData.startTime).toString()
            data[WindowAppearanceTimeAttribute] = appearanceTime
        }

        if (completionStatsData.finishReason != null) {
            data[FinishReason] = completionStatsData.finishReason.toString()
        }

        if (completionStatsData.invocationCount != null) {
            data[InvocationCount] = completionStatsData.invocationCount.toString()
        }

        if (completionStatsData.selectedItem != null) {
            data[ChoiceAtPositionAttribute] = completionStatsData.selectedItem.toString()
            data[CompletionEventAttribute] = Chosen
        } else {
            data[CompletionEventAttribute] = NotChosen
        }

        // sending the data was turned of for 1.3.61+ as we sent enough in 1.3.60
        // KotlinFUSLogger.log(FUSEventGroups.Editor, EventName, data)
    }
}

enum class FileTypeStats {
    KT, GRADLEKTS, KTS
}

enum class CompletionTypeStats {
    BASIC, SMART
}

enum class FinishReasonStats {
    DONE, CANCELLED, HIDDEN, INTERRUPTED
}