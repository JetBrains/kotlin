/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.spec

import groovy.util.Node
import groovy.util.XmlParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.BufferedInputStream
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

object HtmlSpecLoader {
    private const val SPEC_DOCS_TC_CONFIGURATION_ID = "Kotlin_Spec_DocsMaster"
    private const val TC_URL = "https://teamcity.jetbrains.com"
    private const val TC_PATH_PREFIX = "guestAuth/app/rest/builds"
    private const val HTML_SPEC_PATH = "/web/kotlin-spec.html"
    private const val STABLE_BRANCH = "master"

    private fun loadRawHtmlSpec(specVersion: String, buildNumber: String): String {
        val htmlSpecLink =
            "$TC_URL/$TC_PATH_PREFIX/buildType:(id:$SPEC_DOCS_TC_CONFIGURATION_ID),number:$buildNumber,branch:default:any/artifacts/content/kotlin-spec-$specVersion-$buildNumber.zip%21$HTML_SPEC_PATH"

        return BufferedInputStream(URL(htmlSpecLink).openStream()).readBytes().toString(Charset.forName("UTF-8"))
    }

    private fun getLastSpecVersion(): Pair<String, String> {
        val xmlParser = XmlParser()
        val buildInfo =
            xmlParser.parse("$TC_URL/$TC_PATH_PREFIX/buildType:(id:$SPEC_DOCS_TC_CONFIGURATION_ID),count:1,status:SUCCESS?branch=$STABLE_BRANCH")
        val artifactsLink = (buildInfo.children().find { (it as Node).name() == "artifacts" } as Node).attribute("href").toString()
        val artifacts = xmlParser.parse(TC_URL + artifactsLink)
        val artifactName = (artifacts.children().single() as Node).attribute("name").toString()
        val artifactNameMatches =
            Pattern.compile("""kotlin-spec-(?<specVersion>\d+\.\d+)-(?<buildNumber>[1-9]\d*)\.zip""").matcher(artifactName).apply { find() }

        return Pair(artifactNameMatches.group("specVersion"), artifactNameMatches.group("buildNumber"))
    }

    private fun parseHtmlSpec(htmlSpecContent: String) = Jsoup.parse(htmlSpecContent).body()

    fun loadSpec(version: String): Element? {
        val specVersion = version.substringBefore("-")
        val buildNumber = version.substringAfter("-")

        return parseHtmlSpec(loadRawHtmlSpec(specVersion, buildNumber))
    }

    fun loadLatestSpec(): Pair<String, Element?> {
        val (specVersion, buildNumber) = getLastSpecVersion()

        return Pair("$specVersion-$buildNumber", parseHtmlSpec(loadRawHtmlSpec(specVersion, buildNumber)))
    }
}