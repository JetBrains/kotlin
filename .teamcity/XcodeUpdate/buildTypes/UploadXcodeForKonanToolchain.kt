package XcodeUpdate.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object UploadXcodeForKonanToolchain : BuildType({
    name = "🍎 Upload Xcode for Konan Toolchain"
    description = """
        Downloads Xcode, splits it into parts and uploads for use by KONAN_USE_INTERNAL_SERVER
        
        Specify the following environment variables:
        XCODE_XIP_URL               - Xcode.xip URL, e.g. https://repo.labs.intellij.net/artifactory/xcode-distr/Xcode_14_1_rc2.xip
        KONAN_TOOLCHAIN_UPLOAD_URL  - URL for Xcode parts to upload to, e.g. https://repo.labs.intellij.net/kotlin-native
    """.trimIndent()

    params {
        param("env.XCODE_XIP_URL", "https://repo.labs.intellij.net/artifactory/xcode-distr/")
        param("env.KONAN_TOOLCHAIN_UPLOAD_URL", "https://repo.labs.intellij.net/kotlin-native/")
    }

    steps {
        script {
            name = "Process and upload Xcode"
            scriptContent = """
                #!/usr/bin/python3
                import itertools
                import json
                import os
                import shutil
                import subprocess
                from pathlib import Path
                from typing import List
                from tempfile import TemporaryDirectory
                from urllib.parse import urljoin
                
                
                def main():
                    '''
                    This script downloads Xcode, splits it into parts and uploads for use by KONAN_USE_INTERNAL_SERVER
                
                    Specify the following environment variables:
                    XCODE_XIP_URL               - Xcode.xip URL, e.g. https://repo.labs.intellij.net/artifactory/xcode-distr/Xcode_14_1_rc2.xip
                    KONAN_TOOLCHAIN_UPLOAD_URL  - URL for Xcode parts to upload to, e.g. https://repo.labs.intellij.net/kotlin-native/
                    '''
                    xcode_xip_url = os.environ['XCODE_XIP_URL']
                    konan_toolchain_upload_url = os.environ['KONAN_TOOLCHAIN_UPLOAD_URL']
                    # Path needs to end with a / for urljoin
                    if konan_toolchain_upload_url[-1] != '/':
                        konan_toolchain_upload_url += '/'
                
                    with TemporaryDirectory() as temporary_directory:
                        temporary_directory_path = Path(temporary_directory)
                        xip_path = temporary_directory_path.joinpath('Xcode.xip')
                        print('Downloading Xcode')
                        subprocess.check_output(args=['/usr/bin/curl', '-vfsSL', xcode_xip_url, '-o', xip_path])
                        print('Unzipping...')
                        subprocess.check_output(args=['/usr/bin/xip', '--expand', xip_path], cwd=temporary_directory_path)
                        apps = [file for file in temporary_directory_path.iterdir() if file.suffix == '.app']
                        if len(apps) != 1:
                            raise Exception(f'Expected to find single Xcode.app, but found: {apps}')
                        xcode_path = apps[0]
                
                        accept_license(xcode_path)
                        konan_toolchain_version = xcode_version(xcode_path)
                
                        files_to_upload: List[Path] = list()
                        sdks = ['macosx', 'iphoneos', 'iphonesimulator', 'appletvos', 'appletvsimulator', 'watchos', 'watchsimulator']
                        for sdk in sdks:
                            sdk_tarball_name = f'target-sysroot-{konan_toolchain_version}-{sdk}'
                            sdk_tarball_output_path = temporary_directory_path.joinpath(f'{sdk_tarball_name}.tar.gz')
                            files_to_upload.append(sdk_tarball_output_path)
                            pack(sdk_tarball_output_path, [sdk_sysroot_path(sdk, xcode_path)], sdk_tarball_name, 'SDK')
                
                        extra_frameworks_path = temporary_directory_path.joinpath('FrameworksToPack/ExtraFrameworks')
                        extra_frameworks_path.mkdir(parents=True)
                        llbuild_path = llbuild_framework_path(xcode_path)
                        shutil.copytree(llbuild_path, extra_frameworks_path.joinpath(llbuild_path.name))
                
                        toolchain_bin_tarball_name = f'target-toolchain-{konan_toolchain_version}'
                        toolchain_bin_tarball_output_path = temporary_directory_path.joinpath(f'{toolchain_bin_tarball_name}.tar.gz')
                        pack(toolchain_bin_tarball_output_path, [toolchain_bin_path(xcode_path), extra_frameworks_path.parent], toolchain_bin_tarball_name, 'toolchain')
                        files_to_upload.append(toolchain_bin_tarball_output_path)
                
                        xcode_bin_tarball_name = f'xcode-addon-{konan_toolchain_version}'
                        xcode_bin_tarball_output_path = temporary_directory_path.joinpath(f'{xcode_bin_tarball_name}.tar.gz')
                        files_to_upload.append(xcode_bin_tarball_output_path)
                        pack(xcode_bin_tarball_output_path, [xcode_bin_path(xcode_path)], xcode_bin_tarball_name, 'additional tools')
                
                        for file in files_to_upload:
                            print(f'Uploading {file}')
                            subprocess.check_output(args=['/usr/bin/curl', '-vfsSL', '-X', 'PUT', '-T', file,
                                                 urljoin(konan_toolchain_upload_url, file.name)])
                
                
                def pack(output_tarball_path: Path, paths_to_pack: List[Path], tarball_name: str, item: str):
                    print(f'Packing {item} from {paths_to_pack} as {tarball_name}')
                    subprocess.check_output(
                        args=[
                            '/usr/bin/tar', 'czf', output_tarball_path,
                            '--options=!timestamp', '-s', f'/^\./{tarball_name}/HS',
                        ] + list(itertools.chain.from_iterable([['-C', path, '.'] for path in paths_to_pack]))
                    )
                
                
                def xcode_version(xcode_path: Path) -> str:
                    xcode_version_json = json.loads(
                        subprocess.check_output(
                            args=['/usr/bin/plutil', '-convert', 'json', xcode_path.joinpath('Contents/version.plist'), '-o', '-']
                        )
                    )
                    return f"xcode_{xcode_version_json['CFBundleShortVersionString']}_{xcode_version_json['ProductBuildVersion']}"
                
                
                def llbuild_framework_path(xcode_path: Path) -> Path:
                    return xcode_path.joinpath('Contents/SharedFrameworks/llbuild.framework')
                
                
                def sdk_sysroot_path(sdk: str, xcode_path: Path) -> Path:
                    return xcrun_path(['--sdk', sdk, '--show-sdk-path'], xcode_path)
                
                
                def toolchain_bin_path(xcode_path: Path) -> Path:
                    return xcrun_path(['-f', 'ld'], xcode_path).parent.parent.parent
                
                
                def xcode_bin_path(xcode_path: Path) -> Path:
                    return xcrun_path(['-f', 'bitcode-build-tool'], xcode_path).parent.parent
                
                
                def xcrun_path(
                        command: List[str],
                        xcode_path: Path,
                ) -> Path:
                    return Path(
                        subprocess.check_output(
                            args=['/usr/bin/xcrun'] + command,
                            encoding='utf8',
                            env={'DEVELOPER_DIR': xcode_path}
                        )[:-1]
                    )
                
                
                # xcrun doesn't work without license and accepting a license requires root
                def accept_license(xcode_path: Path):
                    subprocess.check_output(
                        args=['sudo', '--preserve-env', '/usr/bin/xcodebuild', '-license', 'accept'],
                        env={'DEVELOPER_DIR': xcode_path}
                    )
                
                
                if __name__ == '__main__':
                    main()
            """.trimIndent()
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        startsWith("teamcity.agent.name", "aquarius-upload-xcode")
    }
})
