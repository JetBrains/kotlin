// JavaScript
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { check } from 'k6';
import { Client, Stream } from 'k6/net/grpc';
import grpc from 'k6/net/grpc';
import encoding from 'k6/encoding';

const client = new Client();

client.load(['src/main/proto'], 'CompileService.proto');

const filePath = "/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/test/resources/TestInput.kt"

const fileMap =  new Map()
fileMap.set(
    filePath,
   open(filePath, 'b')
)

export let options = {
    teardownTimeout: '5m',
    setupTimeout: '5m',
    scenarios: {
        baseline: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            exec: 'compile',
        },
        // stress: {
        //     executor: 'ramping-vus',
        //     startTime: '2m',
        //     exec: 'compile',
        //     stages: [
        //         { duration: '30s', target: 100 },
        //         { duration: '30s', target: 200 },
        //         { duration: '30s', target: 500 },
        //         { duration: '30s', target: 1000 },
        //     ],
        //
        // }
    },
};


export function compile() {
    if (__ITER === 0) {
        client.connect('localhost:50051', { plaintext: true, reflect: false });
    }

    const credential = "YWRtaW46YWRtaW4=" // admin:admin base64 encoded
    const params = {
        metadata: {
            'credential' : `${credential}`,
        },
    };

    const stream = new Stream(client, 'org.jetbrains.kotlin.server.CompileService/Compile', params);

    let compiledChunksCounter = 0;

    stream.on('data',  (data) => {
        if (data.fileTransferReply != null && data.fileTransferReply.isPresent == undefined) {
            const content = fileMap.get(data.fileTransferReply.filePath)
            stream.write({
                source_file_chunk: {
                    file_path: data.fileTransferReply.filePath,
                    content:encoding.b64encode(content),
                    is_last: true
                }
            })
        }
        if(data.compilationResult != null){
            check(data.compilationResult, {
                // 0 is default value for exit code and therefore is not sent
                'compilation exit code == 0': (cr) => cr.exitCode === undefined
            })
        }

        if(data.compiledFileChunk != null){
            compiledChunksCounter++
        }
    })

    // send metadata
    stream.write({
        metadata: {
            project_name: `test-project-vu${__VU}-iter${__ITER}`,
            file_count: 1,
            compiler_arguments: [],
            compilation_options: {
                compiler_mode: 'NON_INCREMENTAL_COMPILER',
                target_platform: 'JVM',
                report_categories: [],
                report_severity: 5,
                requested_compilation_results: [],
                kotlin_script_extensions: [],
            },
        },
    });

    // send file request transfers
    for (const [key, value] of fileMap.entries()) {
        stream.write({
            file_transfer_request: {
                file_path: key,
                file_fingerprint: '1234567890',//TODO
            },
        });
    }

    stream.on('end', function (status) {
        check(null, {
            'received compiled file chunks > 0': () => compiledChunksCounter > 0
        })
    })

    stream.on('error', function (e) {
        console.error(`An error occurred on the gRPC stream: ${e.message}`);
        console.error(`Full error details: ${JSON.stringify(e, null, 2)}`)
    })
}

export function setup() {
    cleanup()
}

export function teardown() {
    cleanup()
}

function cleanup(){
    client.connect('localhost:50051', { plaintext: true, reflect: false });
    const credential = "YWRtaW46YWRtaW4="; // admin:admin base64 encoded
    const params = {
        metadata: {
            credential: `${credential}`,
        },
    };

    const res = client.invoke('org.jetbrains.kotlin.server.CompileService/Cleanup', {}, params);
    check(res, {
        'cleanup OK': (r) => r && r.status === grpc.StatusOK,
    });
}