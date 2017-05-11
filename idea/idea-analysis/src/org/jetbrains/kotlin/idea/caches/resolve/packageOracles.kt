/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.PackageOracle
import org.jetbrains.kotlin.analyzer.PackageOracleFactory
import org.jetbrains.kotlin.idea.caches.PerModulePackageCacheService
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class IdePackageOracleFactory(val project: Project) : PackageOracleFactory {
    override fun createOracle(moduleInfo: ModuleInfo): PackageOracle {
        if (moduleInfo !is IdeaModuleInfo) return PackageOracle.Optimistic

        when {
            moduleInfo.platform == JvmPlatform -> when (moduleInfo.moduleOrigin) {
                ModuleOrigin.LIBRARY -> return JavaPackagesOracle(moduleInfo, project)
                ModuleOrigin.MODULE -> return JvmSourceOracle(moduleInfo as ModuleSourceInfo, project)
                ModuleOrigin.OTHER -> return PackageOracle.Optimistic
            }
            else -> when (moduleInfo.moduleOrigin) {
                ModuleOrigin.MODULE -> return KotlinSourceFilesOracle(moduleInfo as ModuleSourceInfo)
                else -> return PackageOracle.Optimistic // binaries for non-jvm platform need some oracles based on their structure
            }
        }
    }

    private class JavaPackagesOracle(moduleInfo: IdeaModuleInfo, project: Project) : PackageOracle {
        private val scope = moduleInfo.contentScope()
        private val facade = project.service<KotlinJavaPsiFacade>()

        override fun packageExists(fqName: FqName) = facade.findPackage(fqName.asString(), scope) != null
    }

    private class KotlinSourceFilesOracle(private val moduleInfo: ModuleSourceInfo) : PackageOracle {
        private val cacheService = moduleInfo.module.project.service<PerModulePackageCacheService>()

        override fun packageExists(fqName: FqName): Boolean {
            return cacheService.packageExists(fqName, moduleInfo)
        }
    }

    private class JvmSourceOracle(moduleInfo: ModuleSourceInfo, project: Project) : PackageOracle {
        private val javaPackagesOracle = JavaPackagesOracle(moduleInfo, project)
        private val kotlinSourceOracle = KotlinSourceFilesOracle(moduleInfo)

        override fun packageExists(fqName: FqName) =
                javaPackagesOracle.packageExists(fqName)
                || kotlinSourceOracle.packageExists(fqName)
                || fqName.isSubpackageOf(ANDROID_SYNTHETIC_PACKAGE_PREFIX)
    }
}

private val ANDROID_SYNTHETIC_PACKAGE_PREFIX = FqName("kotlinx.android.synthetic")