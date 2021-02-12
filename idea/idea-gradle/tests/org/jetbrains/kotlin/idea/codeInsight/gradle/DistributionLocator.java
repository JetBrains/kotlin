/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle;


import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;

public class DistributionLocator {
    private static final String RELEASE_REPOSITORY_ENV = "GRADLE_RELEASE_REPOSITORY";
    private static final String SNAPSHOT_REPOSITORY_ENV = "GRADLE_SNAPSHOT_REPOSITORY";
    private static final String GRADLE_RELEASE_REPO = "https://services.gradle.org/distributions";
    private static final String GRADLE_SNAPSHOT_REPO = "https://services.gradle.org/distributions-snapshots";

    @NotNull private final String myReleaseRepoUrl;
    @NotNull private final String mySnapshotRepoUrl;

    public DistributionLocator() {
        this(getRepoUrl(false), getRepoUrl(true));
    }

    public DistributionLocator(@NotNull String releaseRepoUrl, @NotNull String snapshotRepoUrl) {
        myReleaseRepoUrl = releaseRepoUrl;
        mySnapshotRepoUrl = snapshotRepoUrl;
    }

    @NotNull
    public URI getDistributionFor(@NotNull GradleVersion version) throws URISyntaxException {
        return getDistribution(getDistributionRepository(version), version, "gradle", "bin");
    }

    @NotNull
    private String getDistributionRepository(@NotNull GradleVersion version) {
        return version.isSnapshot() ? mySnapshotRepoUrl : myReleaseRepoUrl;
    }

    private static URI getDistribution(
            @NotNull String repositoryUrl,
            @NotNull GradleVersion version,
            @NotNull String archiveName,
            @NotNull String archiveClassifier
    ) throws URISyntaxException {
        return new URI(String.format("%s/%s-%s-%s.zip", repositoryUrl, archiveName, version.getVersion(), archiveClassifier));
    }

    @NotNull
    public static String getRepoUrl(boolean isSnapshotUrl) {
        String envRepoUrl = System.getenv(isSnapshotUrl ? SNAPSHOT_REPOSITORY_ENV : RELEASE_REPOSITORY_ENV);
        if (envRepoUrl != null) return envRepoUrl;

        return isSnapshotUrl ? GRADLE_SNAPSHOT_REPO : GRADLE_RELEASE_REPO;
    }
}
